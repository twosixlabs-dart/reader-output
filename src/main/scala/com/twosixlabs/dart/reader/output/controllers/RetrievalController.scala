package com.twosixlabs.dart.reader.output.controllers

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.reader.output.services.query.ReaderSearchService
import com.twosixlabs.dart.reader.output.services.storage.ReaderOutputStorageService
import com.twosixlabs.dart.exceptions.ExceptionImplicits.FutureExceptionLogging
import com.twosixlabs.dart.exceptions.ResourceNotFoundException
import com.twosixlabs.dart.rest.scalatra.AsyncDartScalatraServlet
import org.scalatra.{ActionResult, AsyncResult, Ok}
import org.scalatra.servlet.FileUploadSupport

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class RetrievalController( dependencies : RetrievalController.Dependencies )
  extends AsyncDartScalatraServlet
    with SecureDartController
    with FileUploadSupport {

    override val serviceName : String = dependencies.serviceName
    override val secretKey : Option[String ] = dependencies.authDependencies.secretKey
    override val useDartAuth: Boolean = dependencies.authDependencies.useBasicAuth
    override val basicAuthCredentials: Seq[ (String, String) ] = dependencies.authDependencies.basicAuthCredentials

    import dependencies._

    override protected implicit def executor : ExecutionContext = executionContext

    get( "/:storageKey" ) ( AuthenticateRoute.withUser { _ =>
        logRoute( request )

        new AsyncResult { val is : Future[ ActionResult ] = {
            val storageKey = params( "storageKey" )

            ( for {
                _ <- searchService.exists( storageKey ) transform {
                    case Success( true ) => Success()
                    case Success( false ) => Failure( new ResourceNotFoundException( "storage key", Some( storageKey ) ) )
                    case fail@Failure( _ ) => fail
                }
                file <- storageService.retrieve( storageKey )
            } yield file ) transformWith {
                case Success( file ) =>
                    response.setHeader( "Content-Disposition", f"""attachment; filename="${storageKey}"""" )
                    contentType = "application/octet-stream"
                    Future.successful( Ok( file ) )
                case Failure( e ) =>
                    contentType = "application/json"
                    handleOutput( throw e )
            }
        } }

    } )
}

object RetrievalController {
    case class Dependencies( searchService : ReaderSearchService,
                             storageService : ReaderOutputStorageService,
                             authDependencies : AuthDependencies,
                             serviceName : String = "readers",
                             executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) {
        def build() : RetrievalController = new RetrievalController( this )
    }

    def apply( searchService : ReaderSearchService,
               storageService : ReaderOutputStorageService,
               authDependencies : AuthDependencies,
               serviceName : String = "readers",
               executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) : RetrievalController =
        Dependencies( searchService, storageService, authDependencies, serviceName, executionContext ).build()
}
