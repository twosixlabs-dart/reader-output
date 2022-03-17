package com.twosixlabs.dart.reader.output.services.db

import com.twosixlabs.dart.arangodb.tables.{CanonicalDocsTable, TenantDocsTables}
import com.twosixlabs.dart.test.base.StandardTestBase3x
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Future

class ArangoDartDatastoreTestSuite extends StandardTestBase3x with BeforeAndAfterAll {

    val canonicalDocsTable : CanonicalDocsTable = mock[ CanonicalDocsTable ]
    val tenantsTable       : TenantDocsTables  = mock[ TenantDocsTables ]

    override def beforeAll( ) : Unit = reset( canonicalDocsTable, tenantsTable )

    "Cassandra DART Datastore" should "return a map with all document Ids with tenant Ids" in {
        val dartDatastore : ArangoDartDatastore = ArangoDartDatastore( tenantsTable )
        when( tenantsTable.getTenants ).thenReturn( Future.successful( Iterator( "tenant-1", "tenant-2" ) ) )
        when( tenantsTable.getDocsByTenant( "tenant-1" ) ).thenReturn( Future.successful( Iterator( "doc-1", "doc-2" ) ) )
        when( tenantsTable.getDocsByTenant( "tenant-2" ) ).thenReturn( Future.successful( Iterator( "doc-1", "doc-4" ) ) )

        val docsAndTenants = dartDatastore.getAllDocsAndTenants
        docsAndTenants shouldBe Map( "doc-1" -> Set( "tenant-1", "tenant-2" ), "doc-2" -> Set( "tenant-1" ), "doc-4" -> Set( "tenant-2" ) )
    }
}
