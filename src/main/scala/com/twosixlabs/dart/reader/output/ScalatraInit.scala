package com.twosixlabs.dart.reader.output

import com.twosixlabs.dart.arangodb.Arango
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.reader.output.controllers.{QueryController, ReaderController, RetrievalController}
import com.twosixlabs.dart.reader.output.services.db.ArangoDartDatastore
import com.twosixlabs.dart.reader.output.services.metadata.PgReaderOutputMetadataService
import com.twosixlabs.dart.reader.output.services.notification._
import com.twosixlabs.dart.reader.output.services.notification.kafka.KafkaNotificationService
import com.twosixlabs.dart.reader.output.services.query.PgArangoReaderSearchService
import com.twosixlabs.dart.reader.output.services.storage._
import com.twosixlabs.dart.reader.output.services.submission.DartReaderOutputSubmissionService
import com.twosixlabs.dart.operations.status.client.PipelineStatusUpdateClient
import com.twosixlabs.dart.rest.ApiStandards
import com.twosixlabs.dart.rest.scalatra.DartRootServlet
import com.typesafe.config.ConfigFactory
import org.scalatra.LifeCycle
import org.slf4j.{Logger, LoggerFactory}

import javax.servlet.ServletContext
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class ScalatraInit extends LifeCycle {

    // All local variables here should be lazy so that any exceptions thrown will
    // be caught when the init() method is called

    private lazy val LOG : Logger = LoggerFactory.getLogger( getClass )

    private lazy val config = ConfigFactory.defaultApplication().resolve()

    import com.twosixlabs.dart.reader.output.configuration.PgSlickProfile.api._

    def TryOrExit[ T ]( attempt : => T ) : T = Try( attempt ) match {
        case Success( res ) => res
        case Failure( e ) =>
            LOG.error( "EXCEPTION ON CONFIGURATION -- EXITING:" )
            LOG.error( e.getClass.getCanonicalName )
            LOG.error( e.getMessage )
            e.printStackTrace()
            System.exit( 1 )
            throw e
    }

    import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors._

    private lazy val authDeps       : AuthDependencies           = config.build[ AuthDependencies ]
    private lazy val db             : Database                   = config.build[ Database ]
    private lazy val arango         : Arango                     = config.build[ Arango ]
    private lazy val storageService : ReaderOutputStorageService = config.build[ ReaderOutputStorageService ]
    private lazy val opsClient      : PipelineStatusUpdateClient = config.build[ PipelineStatusUpdateClient ]

    private lazy val notifications : Seq[ NotificationService ] = TryOrExit {
        val kafkaNotifiers =
            if ( config.hasPath( "notification.kafka.topic" ) ) Seq( config.build[ KafkaNotificationService ] )
            else Nil

        val restNotifiers = {
            val timeout = Try( config.getLong( "notification.rest.timeout" ) ).toOption

            val urls : Seq[ String ] = Try( config.getStringList( "notification.rest.urls" ).asScala ).toOption
              .getOrElse( Try( config.getString( "notification.rest.urls" ).split( ',' ).toSeq ).toOption.getOrElse( Nil ) )

            urls.map( url => {
                timeout match {
                    case None => RestNotificationService( url )
                    case Some( to ) => RestNotificationService( url, to )
                }
            } )
        }

        val operationsNotifier = OperationsNotificationService( opsClient )

        kafkaNotifiers ++ restNotifiers :+ operationsNotifier
    }

    LOG.info( s"Registered ${notifications.size} notification services" )

    private lazy val metadataSubmissionService = PgReaderOutputMetadataService( db )

    private lazy val searchService : PgArangoReaderSearchService =
        PgArangoReaderSearchService( ArangoDartDatastore( arango, scala.concurrent.ExecutionContext.global ),
                                     db )

    private lazy val readerSubmissionService = DartReaderOutputSubmissionService(
        storageService,
        metadataSubmissionService,
        notifications,
    )

    private lazy val streamFileThresholdMb : Int =
        Try( config.getInt( "file.stream.threshold.mb") ).getOrElse( 100 )

    private lazy val rootServlet = new DartRootServlet( Some( v1BasePath ),
                                                        Some( getClass.getPackage.getImplementationVersion ) )
    private lazy val queryController = QueryController( searchService, authDeps )
    private lazy val readerController = ReaderController(  readerSubmissionService, streamFileThresholdMb, opsClient, authDeps )
    private lazy val retrievalController = RetrievalController( searchService, storageService, authDeps )

    private lazy val v1BasePath = ApiStandards.DART_API_PREFIX_V1 + "/readers"

    // Initialize scalatra: mounts servlets
    override def init( context : ServletContext ) : Unit = TryOrExit {
        context.mount( rootServlet, "/*" )
        context.mount( queryController, v1BasePath + "/query/*" )
        context.mount( readerController, v1BasePath + "/upload/*" )
        context.mount( retrievalController, v1BasePath + "/download/*" )
    }

    // Scalatra callback to close out resources
    override def destroy( context : ServletContext ) : Unit = {
        db.close()
        super.destroy( context )
    }

}
