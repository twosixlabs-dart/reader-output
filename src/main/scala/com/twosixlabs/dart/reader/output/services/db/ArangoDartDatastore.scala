package com.twosixlabs.dart.reader.output.services.db

import com.twosixlabs.dart.arangodb.Arango
import com.twosixlabs.dart.arangodb.tables.TenantDocsTables
import com.twosixlabs.dart.reader.output.exceptions.{UnableToGetAllTenants, UnableToGetDocIdsByTenant}
import com.twosixlabs.dart.utils.AsyncDecorators.DecoratedFuture
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

class ArangoDartDatastore( dependencies : ArangoDartDatastore.Dependencies ) {
    import dependencies._

    implicit private lazy val ec : ExecutionContext = executionContext
    private val LOG = LoggerFactory.getLogger( getClass )


    def getAllDocsAndTenants : Map[ String, Set[String] ] = {

        tenantsDocsTables.getTenants synchronously ( 86400000 ) match {
            case Success( allTenants ) => {
                allTenants.flatMap( currentTenant => {
                    val docIds = tenantsDocsTables.getDocsByTenant( currentTenant ) synchronously ( 86400000 ) match {
                        case Success( allDocuments ) => allDocuments
                        case Failure( e ) =>
                            LOG.error( s"Unable to retrieve document Ids for tenant: ${currentTenant}, Error: ${e.getLocalizedMessage}" )
                            throw new UnableToGetDocIdsByTenant( currentTenant )
                    }
                    docIds.map(docId => (docId, currentTenant))
                } ).foldLeft(Map.empty[String, Set[String]]) { ( docMap : Map[String, Set[String ] ], docTuple : (String, String) ) =>
                    val (docId : String, tenantId : String) = docTuple

                    if(docMap.contains(docId)) {
                        docMap + ((docId, docMap( docId ) + tenantId))
                    } else {
                        docMap + ((docId, Set(tenantId)))
                    }
                }
            }
            case Failure( e ) =>
                LOG.error( s"Unable to retrieve tenants, Error: ${e.getLocalizedMessage}" )
                throw new UnableToGetAllTenants
        }
    }
}
object ArangoDartDatastore {
    case class Dependencies( tenantsDocsTables : TenantDocsTables,
        executionContext : ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) {
        def build() : ArangoDartDatastore = new ArangoDartDatastore( this )
    }

    def apply( tenantsDocsTables : TenantDocsTables,
        executionContext : ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) : ArangoDartDatastore = Dependencies( tenantsDocsTables, executionContext ).build()

    def apply( arangodb : Arango,
        // can't have multiple overloaded methods with default args
        executionContext : ExecutionContextExecutor ) : ArangoDartDatastore =
        apply( new TenantDocsTables( arangodb ), executionContext )
}

