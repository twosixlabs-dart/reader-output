package com.twosixlabs.dart.reader.output.services.query

import com.twosixlabs.dart.reader.output.configuration.PgSlickProfile.api._
import com.twosixlabs.dart.reader.output.models.QueryModels.{ReaderOutputMetadataQueryResult, ReaderOutputQuery}
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputMetadataRecord
import com.twosixlabs.dart.reader.output.models.tables.Schema.readerOutputTableQuery
import com.twosixlabs.dart.reader.output.services.db.ArangoDartDatastore

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PgArangoReaderSearchService( dependencies : PgArangoReaderSearchService.Dependencies )
  extends ReaderSearchService {

    import dependencies._

    override def search( query : ReaderOutputQuery ) : Future[ Seq[ ReaderOutputMetadataQueryResult ] ] = {
        lazy val docTenantMap = arangoStore.getAllDocsAndTenants

        val searchQuery = readerOutputTableQuery
          .filterOpt( query.documentIds )( _.documentId.inSetBind( _ ) )
          .filterOpt( query.labels )( _.labels @> _.bind )
          .filterOpt( query.outputVersions )( _.outputVersion.inSetBind( _ ) )
          .filterOpt( query.readers )( _.readerId.inSetBind( _ ) )
          .filterOpt( query.timestamp.flatMap( x => Option( x.after ) ) )( _.timestamp > _ )
          .filterOpt( query.timestamp.flatMap( x => Option( x.before ) ) )( _.timestamp < _ )
          .filterOpt( query.timestamp.flatMap( x => Option( x.on ) ) )( _.timestamp === _ )
          .filterOpt( query.versions )( _.readerVersion.inSetBind( _ ) )
          .result

        database.run( searchQuery ).map( results => {
            // Use flatMap to transform and filter records at the same time
            results.flatMap( ( readerRecord : ReaderOutputMetadataRecord ) => {
                val actualTenants =
                    docTenantMap
                      .get( readerRecord.documentId )
                      .toList
                      .flatMap( _.toList )
                val readerRes = readerRecord.toQueryResult( actualTenants )

                query.tenantId match {
                    case None => Some( readerRes )
                    case Some( queriedTenant ) =>
                        if ( actualTenants.contains( queriedTenant ) )
                            Some( readerRes )
                        else None
                }
            } )
        } )
    }

    override def exists( storageKey : String ) : Future[ Boolean ] = {
        database.run(
            readerOutputTableQuery
              .filter( _.storageKey === storageKey )
              .take( 1 )
              .map( _.storageKey )
              .result
            ).map( _.nonEmpty )
    }
}

object PgArangoReaderSearchService {

    case class Dependencies( arangoStore : ArangoDartDatastore,
                             database : Database ) {
        def build( ) : PgArangoReaderSearchService = new PgArangoReaderSearchService( this )
    }

    def apply( arangoStore : ArangoDartDatastore, database : Database ) : PgArangoReaderSearchService =
        Dependencies( arangoStore, database ).build()
}
