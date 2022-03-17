package com.twosixlabs.dart.reader.output.controllers

import com.twosixlabs.dart.auth.controllers.SecureDartController
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.reader.output.models.QueryModels.{ReaderOutputQuery, ReaderOutputQueryResults}
import com.twosixlabs.dart.reader.output.services.query.ReaderSearchService
import com.twosixlabs.dart.exceptions.BadRequestBodyException
import com.twosixlabs.dart.json.JsonFormat.unmarshalTo
import com.twosixlabs.dart.rest.scalatra.AsyncDartScalatraServlet
import org.scalatra.servlet.FileUploadSupport

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class QueryController( dependencies : QueryController.Dependencies )
  extends AsyncDartScalatraServlet
    with SecureDartController
    with FileUploadSupport {

    override val serviceName : String = dependencies.serviceName
    override val secretKey : Option[String ] = dependencies.authDependencies.secretKey
    override val useDartAuth: Boolean = dependencies.authDependencies.useBasicAuth
    override val basicAuthCredentials: Seq[ (String, String) ] = dependencies.authDependencies.basicAuthCredentials

    import dependencies._

    override protected implicit def executor : ExecutionContext = scala.concurrent.ExecutionContext.global

    post( "/" )( handleOutput ( AuthenticateRoute.withUser { _ =>
        logRoute( request )
        contentType = "application/json;charset=utf-8"

        val query : ReaderOutputQuery = params.get( "metadata" ) match {
            case Some( metadata ) =>
                unmarshalTo( metadata, classOf[ ReaderOutputQuery ] ) match {
                    case Success( queryMetadata ) => queryMetadata
                    case Failure( _ ) =>
                        throw new BadRequestBodyException( "metadata was not valid" )
                }
            case _ =>
                throw new BadRequestBodyException( "no metadata provided" )
        }

        val fixedQuery = query.copy( readers = query.readers.map( _.map( _.toLowerCase ) ),
                                     versions = query.versions.map( _.map( _.toLowerCase ) ),
                                     documentIds = query.documentIds.map( _.map( _.toLowerCase ) ),
                                     outputVersions = query.outputVersions.map( _.map( _.toLowerCase ) ),
                                     tenantId = query.tenantId.map( _.toLowerCase ),
                                     labels = query.labels.map( _.map( _.toLowerCase ) ) )

        readerSearch.search( fixedQuery )
          .map( results => ReaderOutputQueryResults( results ) )

    } ) )
}

object QueryController {
    case class Dependencies( readerSearch : ReaderSearchService,
                             authDependencies : AuthDependencies,
                             serviceName : String = "readers" ) {
        def build( ) : QueryController = new QueryController( this )
    }

    def apply( readerSearch : ReaderSearchService,
               authDependencies : AuthDependencies,
               serviceName : String = "readers" ) : QueryController =
        Dependencies( readerSearch, authDependencies, serviceName ).build()
}
