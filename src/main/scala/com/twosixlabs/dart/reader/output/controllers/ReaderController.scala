package com.twosixlabs.dart.reader.output.controllers

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.reader.output.exceptions.InvalidMetadataException
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputMetadataSubmission
import com.twosixlabs.dart.reader.output.services.submission.DartReaderOutputSubmissionService
import com.twosixlabs.dart.exceptions.BadRequestBodyException
import com.twosixlabs.dart.exceptions.ExceptionImplicits.FutureExceptionLogging
import com.twosixlabs.dart.json.JsonFormat.unmarshalTo
import com.twosixlabs.dart.operations.status.client.PipelineStatusUpdateClient
import com.twosixlabs.dart.rest.scalatra.{AsyncDartScalatraServlet, DartScalatraServlet}
import org.scalatra.Created
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}

import scala.concurrent.ExecutionContextExecutor
import scala.util.matching.Regex
import scala.util.{Failure, Success}

class ReaderController( dependencies : ReaderController.Dependencies )
  extends AsyncDartScalatraServlet
    with SecureDartController
    with FileUploadSupport {

    override val serviceName : String = dependencies.serviceName
    override val secretKey : Option[String ] = dependencies.authDependencies.secretKey
    override val useDartAuth: Boolean = dependencies.authDependencies.useBasicAuth
    override val basicAuthCredentials: Seq[ (String, String) ] = dependencies.authDependencies.basicAuthCredentials

    import com.twosixlabs.dart.rest.scalatra.DartScalatraServletMethods.LOG

    import dependencies._
    override implicit val executor : ExecutionContextExecutor = executionContext

    val restNotifications : Boolean = true
    val kafkaNotifications : Boolean = true

    val STREAM_THRESHOLD_SIZE : Int = streamFileThresholdMb * 1000 * 1000
    val md5HashValidator : Regex = "(^[a-z0-9]{32}$)".r

    configureMultipartHandling( MultipartConfig( fileSizeThreshold = Some( STREAM_THRESHOLD_SIZE ) ) )

    post( "/" )( handleOutput ( AuthenticateRoute.withUser { _ =>
        logRoute( request )

        contentType = "application/json;charset=utf-8"
        val file = fileParams.get( "file" ) match {
            case Some( f ) => f
            case None => throw new BadRequestBodyException( "no file provided" )
        }

        val metadata : ReaderOutputMetadataSubmission = params.get( "metadata" ) match {
            case Some( metadata ) =>
                unmarshalTo( metadata, classOf[ ReaderOutputMetadataSubmission ] ) match {
                    case Success( uploadMetadata ) => uploadMetadata
                    case Failure( _ ) =>
                        LOG.error( "Metadata was not valid" )
                        throw new BadRequestBodyException( "metadata was not valid" )
                }
            case _ =>
                LOG.error( "Metadata was not provided" )
                throw new BadRequestBodyException( "no metadata provided" )
        }

        readerService
          .submit( file.getName, file.get(), metadata )
          .transform( {
              case res@Success( _ ) => res
              case Failure( e : InvalidMetadataException ) => Failure( new BadRequestBodyException( e.getMessage ) )
              case fail@Failure( _ ) => fail
          } )
          .map( results => Created( results ) )
    } ) )
}

object ReaderController {
    case class Dependencies( readerService : DartReaderOutputSubmissionService,
                             streamFileThresholdMb : Int,
                             opsClient : PipelineStatusUpdateClient,
                             authDependencies : AuthDependencies,
                             serviceName : String = "readers",
                             executionContext : ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) {
        def build() : ReaderController = new ReaderController( this )
    }

    def apply( readerService : DartReaderOutputSubmissionService,
               streamFileThresholdMb : Int,
               opsClient : PipelineStatusUpdateClient,
               authDependencies : AuthDependencies,
               serviceName : String = "readers",
               executionContext : ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) : ReaderController =
        Dependencies( readerService,
                      streamFileThresholdMb,
                      opsClient,
                      authDependencies,
                      serviceName,
                      executionContext ).build()
}
