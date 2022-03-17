package com.twosixlabs.dart.reader.output.services.notification.kafka

import com.twosixlabs.dart.reader.output.models.ReaderModels.{ReaderOutputMetadataSubmission, ReaderOutputSubmissionResult}
import com.twosixlabs.dart.reader.output.services.notification.NotificationService
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.slf4j.{Logger, LoggerFactory}

import java.util.Properties
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class KafkaNotificationService( dependencies : KafkaNotificationService.Dependencies ) extends NotificationService {

    import dependencies.{properties, topic}

    implicit val executionContext : ExecutionContext = dependencies.executionContext

    private val LOG : Logger = LoggerFactory.getLogger( getClass )
    private val kafkaProps : Properties = properties

    private lazy val producer : KafkaProducer[ String, ReaderOutputSubmissionResult ] = new KafkaProducer[ String, ReaderOutputSubmissionResult ]( kafkaProps )

    override def notify( attempt : Either[ (Throwable, ReaderOutputMetadataSubmission), ReaderOutputSubmissionResult ] ) : Unit = attempt match {
        case Left( _ ) =>
        case Right( value ) =>
            val key = value.documentId

            LOG.info( s"sending message : ${key}" )
            val message = new ProducerRecord[ String, ReaderOutputSubmissionResult ]( topic, key, value )

            //@formatter:off
            Try( producer.send( message ).get ) match {
                    case Success( s ) => LOG.info( s"Message ${key} successfully sent" )
                    case Failure( e ) => {
                        LOG.error( s"${e.getClass.getSimpleName} : ${e.getMessage} : ${e.getCause}" )
                        e.printStackTrace()
                    }
            }
            //@formatter:on
    }

}

object KafkaNotificationService {
    case class Dependencies( topic : String,
                             properties : Properties,
                             executionContext : ExecutionContext = scala.concurrent.ExecutionContext.global ) {
        def build( ) : KafkaNotificationService = new KafkaNotificationService( this )
    }

    def apply( topic : String,
               properties : Properties,
               executionContext : ExecutionContext = scala.concurrent.ExecutionContext.global ) : KafkaNotificationService =
        Dependencies( topic, properties, executionContext ).build()
}
