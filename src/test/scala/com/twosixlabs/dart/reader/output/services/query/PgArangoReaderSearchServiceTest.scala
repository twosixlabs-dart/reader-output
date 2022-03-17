package com.twosixlabs.dart.reader.output.services.query

import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors._
import com.twosixlabs.dart.reader.output.configuration.PgSlickProfile.api._
import com.twosixlabs.dart.reader.output.models.ReaderModels
import com.twosixlabs.dart.reader.output.models.tables.Schema
import com.twosixlabs.dart.reader.output.services.db.ArangoDartDatastore
import com.twosixlabs.dart.test.tags.annotations.IntegrationTest
import com.twosixlabs.dart.test.utilities.TestUtils
import org.scalamock.scalatest.MockFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

object PgArangoReaderSearchServiceTest extends MockFactory {
    val config = TestUtils.getConfig
    val db : Database = config.build[ Database ]

    val arangoStoreStub : ArangoDartDatastore = stub[ ArangoDartDatastore ]

    val searchDependencies : PgArangoReaderSearchService.Dependencies = PgArangoReaderSearchService.Dependencies( arangoStoreStub, db )

    val pgCassandraReaderSearch : PgArangoReaderSearchService = new PgArangoReaderSearchService( searchDependencies )
}

@IntegrationTest
class PgArangoReaderSearchServiceTest extends ReaderSearchServiceTest( PgArangoReaderSearchServiceTest.pgCassandraReaderSearch ) {

    import com.twosixlabs.dart.reader.output.services.query.PgArangoReaderSearchServiceTest._

    override def addRecordsToSearchStore( rs : Seq[ ReaderModels.ReaderOutputMetadataRecord ] ) : Future[ Unit ] = {
        db.run( Schema.readerOutputTableQuery ++= rs )
          .map( _ => () )
    }

    override def removeRecordsFromSearchStore(
      rs : Seq[ ReaderModels.ReaderOutputMetadataRecord ] ) : Future[ Unit ] = {

        db.run( Schema.readerOutputTableQuery.delete )
          .map( _ => () )
    }

    override def useDocMap( tenantMap : Map[ String, Seq[ String ] ] ) : Future[ Unit ] = {
        Future {
            ( arangoStoreStub.getAllDocsAndTenants _ ).when().returns( data.tenantMap.mapValues( _.toSet ) )
        }
    }

    override def beforeAll( ) : Unit = {
        Await.result( db.run( Schema.readerOutputTableQuery.schema.createIfNotExists ), 5000.milliseconds )
        Await.result( db.run( Schema.readerOutputTableQuery.schema.truncate ), 10000.milliseconds )

        super.beforeAll()
    }

}
