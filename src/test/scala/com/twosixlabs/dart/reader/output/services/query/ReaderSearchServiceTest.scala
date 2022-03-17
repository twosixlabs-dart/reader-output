package com.twosixlabs.dart.reader.output.services.query

import com.twosixlabs.dart.reader.output.models.QueryModels.{ReaderOutputQuery, TimestampQuery}
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputMetadataRecord
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.test.utilities.TestUtils.FutureImplicits
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.Future

abstract class ReaderSearchServiceTest( readerSearch : ReaderSearchService, timeout : Long = 5000, writeDelay : Long = 100 ) extends StandardTestBase3x with BeforeAndAfterAll with Matchers {

    // So subtests can access the data...
    val data : ReaderSearchTestData.type = ReaderSearchTestData

    import data._

    def addRecordsToSearchStore( rs : Seq[ ReaderOutputMetadataRecord ] ) : Future[ Unit ]

    def removeRecordsFromSearchStore( rs : Seq[ ReaderOutputMetadataRecord ] ) : Future[ Unit ]

    def useDocMap( tenantMap : Map[ String, Seq[ String ] ] ) : Future[ Unit ]

    val futureImplicits = new FutureImplicits( timeout, writeDelay )

    import futureImplicits._

    override def beforeAll( ) : Unit = {
        super.beforeAll()
        addRecordsToSearchStore( allRecords ).awaitWrite
        useDocMap( tenantMap ).awaitWrite
    }

    override def afterAll( ) : Unit = {
        removeRecordsFromSearchStore( allRecords ).awaitWrite
        super.afterAll()
    }

    behavior of "empty query"

    it should "return all results" in {
        val results = readerSearch.search( ReaderOutputQuery() ).await

        results.length shouldBe 27

        readerIdentities.foreach( rId => results.exists( _.identity == rId ) shouldBe true )
        docIds.foreach( dId => results.exists( _.documentId == dId ) shouldBe true )
        outputVersions.foreach( ov => results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        data.tenantMap.values.foreach( tl => results.exists( _.tenants.toSet == tl.toSet ) )
        results.forall( _.tenants.exists( _.nonEmpty ) == true )
        labelLists.foreach( ll => results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => results.exists( _.timestamp == ts ) )
    }

    behavior of "query by readerId"

    it should "filter out records without the given single readerId" in {
        val results = readerSearch.search( ReaderOutputQuery( readers = Some( List( readerIdentity1 ) ) ) ).await

        results.length shouldBe 9

        results.forall( _.identity == readerIdentity1 ) shouldBe true

        docIds.foreach( dId => results.exists( _.documentId == dId ) shouldBe true )
        outputVersions.foreach( ov => results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        data.tenantMap.values.foreach( tl => results.exists( _.tenants.toSet == tl.toSet ) )
        results.forall( _.tenants.exists( _.nonEmpty ) == true )
        labelLists.foreach( ll => results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => results.exists( _.timestamp == ts ) )
    }

    it should "filter out records without at least one of the provided readerIds when more than one" in {
        val results = readerSearch.search( ReaderOutputQuery( readers = Some( List( readerIdentity1, readerIdentity2 ) ) ) ).await

        results.length shouldBe 18

        val rid1Results = results.filter( _.identity == readerIdentity1 )

        rid1Results.length shouldBe 9

        docIds.foreach( dId => rid1Results.exists( _.documentId == dId ) shouldBe true )
        outputVersions.foreach( ov => rid1Results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        data.tenantMap.values.foreach( tl => rid1Results.exists( _.tenants.toSet == tl.toSet ) )
        rid1Results.forall( _.tenants.exists( _.nonEmpty ) == true )
        labelLists.foreach( ll => results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => rid1Results.exists( _.timestamp == ts ) )

        val rid2Results = results.filter( _.identity == readerIdentity2 )

        rid2Results.length shouldBe 9

        docIds.foreach( dId => rid2Results.exists( _.documentId == dId ) shouldBe true )
        outputVersions.foreach( ov => rid2Results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        data.tenantMap.values.foreach( tl => rid2Results.exists( _.tenants.toSet == tl.toSet ) )
        rid2Results.forall( _.tenants.exists( _.nonEmpty ) == true )
        labelLists.foreach( ll => rid2Results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => rid2Results.exists( _.timestamp == ts ) )
    }

    behavior of "query by readerVersion"

    it should "filter out records without the given single readerVersion" in {
        val results = readerSearch.search( ReaderOutputQuery( versions = Some( List( readerVersion1 ) ) ) ).await

        results.length shouldBe 9

        results.forall( _.version == readerVersion1 ) shouldBe true
        results.forall( _.timestamp == ts1 ) shouldBe true
        results.forall( _.outputVersion.contains( outputVersion1 ) ) shouldBe true
        results.forall( _.labels.toSet == labelList1.toSet ) shouldBe true

        docIds.foreach( dId => results.exists( _.documentId == dId ) shouldBe true )
        data.tenantMap.values.foreach( tl => results.exists( _.tenants.toSet == tl.toSet ) )
        results.forall( _.tenants.exists( _.nonEmpty ) == true )
    }

    it should "filter out records without at least one of the provided readerVersions when more than one" in {
        val results = readerSearch.search( ReaderOutputQuery( versions = Some( List( readerVersion1, readerVersion2 ) ) ) ).await

        results.length shouldBe 18

        val rid1Results = results.filter( _.version == readerVersion1 )

        rid1Results.length shouldBe 9

        rid1Results.forall( _.timestamp == ts1 ) shouldBe true
        rid1Results.forall( _.outputVersion.contains( outputVersion1 ) ) shouldBe true
        rid1Results.forall( _.labels.toSet == labelList1.toSet ) shouldBe true

        docIds.foreach( dId => rid1Results.exists( _.documentId == dId ) shouldBe true )
        data.tenantMap.values.foreach( tl => rid1Results.exists( _.tenants.toSet == tl.toSet ) )
        rid1Results.forall( _.tenants.exists( _.nonEmpty ) == true )

        val rid2Results = results.filter( _.version == readerVersion2 )

        rid2Results.length shouldBe 9

        rid2Results.forall( _.timestamp == ts2 ) shouldBe true
        rid2Results.forall( _.outputVersion.contains( outputVersion2 ) ) shouldBe true
        rid2Results.forall( _.labels.toSet == labelList2.toSet ) shouldBe true

        docIds.foreach( dId => rid2Results.exists( _.documentId == dId ) shouldBe true )
        data.tenantMap.values.foreach( tl => rid2Results.exists( _.tenants.toSet == tl.toSet ) )
        rid2Results.forall( _.tenants.exists( _.nonEmpty ) == true )
    }

    behavior of "query by doc id"

    it should "filter out records without the given single doc id" in {
        val results = readerSearch.search( ReaderOutputQuery( documentIds = Some( List( docId1 ) ) ) ).await

        results.length shouldBe 9

        results.forall( _.documentId == docId1 ) shouldBe true

        readerIdentities.foreach( rId => results.exists( _.identity == rId ) shouldBe true )
        outputVersions.foreach( ov => results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        data.tenantMap.values.foreach( tl => results.exists( _.tenants.toSet == tl.toSet ) )
        results.forall( _.tenants.exists( _.nonEmpty ) == true )
        labelLists.foreach( ll => results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => results.exists( _.timestamp == ts ) )
    }

    it should "filter out records without at least one of the provided doc ids when more than one" in {
        val results = readerSearch.search( ReaderOutputQuery( documentIds = Some( List( docId1, docId2 ) ) ) ).await

        results.length shouldBe 18

        val did1Results = results.filter( _.documentId == docId1 )

        did1Results.length shouldBe 9

        readerIdentities.foreach( rId => did1Results.exists( _.identity == rId ) shouldBe true )
        outputVersions.foreach( ov => did1Results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        data.tenantMap.values.foreach( tl => did1Results.exists( _.tenants.toSet == tl.toSet ) )
        did1Results.forall( _.tenants.exists( _.nonEmpty ) == true )
        labelLists.foreach( ll => did1Results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => did1Results.exists( _.timestamp == ts ) )

        val did2Results = results.filter( _.documentId == docId2 )

        did2Results.length shouldBe 9

        readerIdentities.foreach( rId => did1Results.exists( _.identity == rId ) shouldBe true )
        outputVersions.foreach( ov => did2Results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        data.tenantMap.values.foreach( tl => did2Results.exists( _.tenants.toSet == tl.toSet ) )
        did2Results.forall( _.tenants.exists( _.nonEmpty ) == true )
        labelLists.foreach( ll => did2Results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => did2Results.exists( _.timestamp == ts ) )
    }

    behavior of "query by outputVersion"

    it should "filter out records without the given single outputVersion" in {
        val results = readerSearch.search( ReaderOutputQuery( outputVersions = Some( List( outputVersion1 ) ) ) ).await

        results.length shouldBe 9

        results.forall( _.outputVersion.contains( outputVersion1 ) ) shouldBe true
        results.forall( _.version == readerVersion1 ) shouldBe true
        results.forall( _.timestamp == ts1 ) shouldBe true
        results.forall( _.labels.toSet == labelList1.toSet ) shouldBe true

        docIds.foreach( dId => results.exists( _.documentId == dId ) shouldBe true )
        data.tenantMap.values.foreach( tl => results.exists( _.tenants.toSet == tl.toSet ) )
        results.forall( _.tenants.exists( _.nonEmpty ) == true )
    }

    it should "filter out records without at least one of the provided outputVersions when more than one" in {
        val results = readerSearch.search( ReaderOutputQuery( outputVersions = Some( List( outputVersion1, outputVersion2 ) ) ) ).await

        results.length shouldBe 18

        val rid1Results = results.filter( _.outputVersion.contains( outputVersion1 ) )

        rid1Results.length shouldBe 9

        rid1Results.forall( _.version == readerVersion1 ) shouldBe true
        rid1Results.forall( _.timestamp == ts1 ) shouldBe true
        rid1Results.forall( _.labels.toSet == labelList1.toSet ) shouldBe true

        docIds.foreach( dId => rid1Results.exists( _.documentId == dId ) shouldBe true )
        data.tenantMap.values.foreach( tl => rid1Results.exists( _.tenants.toSet == tl.toSet ) )
        rid1Results.forall( _.tenants.exists( _.nonEmpty ) == true )

        val rid2Results = results.filter( _.outputVersion.contains( outputVersion2 ) )

        rid2Results.length shouldBe 9

        rid2Results.forall( _.version == readerVersion2 ) shouldBe true
        rid2Results.forall( _.timestamp == ts2 ) shouldBe true
        rid2Results.forall( _.labels.toSet == labelList2.toSet ) shouldBe true

        docIds.foreach( dId => rid2Results.exists( _.documentId == dId ) shouldBe true )
        data.tenantMap.values.foreach( tl => rid2Results.exists( _.tenants.toSet == tl.toSet ) )
        rid2Results.forall( _.tenants.exists( _.nonEmpty ) == true )
    }

    behavior of "query by labels"

    it should "filter out records that do not contain the given label" in {
        val results = readerSearch.search( ReaderOutputQuery( labels = Some( List( label1 ) ) ) ).await

        results.length shouldBe 9

        results.forall( _.labels.contains( label1 ) ) shouldBe true
        results.forall( _.labels.toSet == labelList1.toSet ) shouldBe true
        results.forall( _.outputVersion.contains( outputVersion1 ) ) shouldBe true
        results.forall( _.version == readerVersion1 ) shouldBe true
        results.forall( _.timestamp == ts1 ) shouldBe true

        docIds.foreach( dId => results.exists( _.documentId == dId ) shouldBe true )
        data.tenantMap.values.foreach( tl => results.exists( _.tenants.toSet == tl.toSet ) )
        results.forall( _.tenants.exists( _.nonEmpty ) == true )
    }

    it should "filter out records that do not contain all of the given labels if more than one label queried" in {
        // labelList1 has label1 and label2; labelList2 is empty; labelList3 has label2 and label3
        // only the first set of records should match, since only the first set has both label1 and label2
        val results = readerSearch.search( ReaderOutputQuery( labels = Some( labelList1 ).map( _.toList ) ) ).await

        results.length shouldBe 9

        results.forall( _.labels.exists( _.contains( label1 ) ) ) shouldBe true
        results.forall( _.labels.toSet == labelList1.toSet ) shouldBe true
        results.forall( _.outputVersion.contains( outputVersion1 ) ) shouldBe true
        results.forall( _.version == readerVersion1 ) shouldBe true
        results.forall( _.timestamp == ts1 ) shouldBe true

        docIds.foreach( dId => results.exists( _.documentId == dId ) shouldBe true )
        data.tenantMap.values.foreach( tl => results.exists( _.tenants.toSet == tl.toSet ) )
        results.forall( _.tenants.exists( _.nonEmpty ) == true )

        readerSearch.search( ReaderOutputQuery( labels = Some( List( label1, label3 ) ) ) ).await should have size 0
    }

    behavior of "query by tenant"

    it should "filter out all records associated with documents not in the queried tenant" in {
        // tenant1 contains only docId1
        val results = readerSearch.search( ReaderOutputQuery( tenantId = Some( tenant1 ) ) ).await
        results.foreach( _.tenants should not have size( 0 ) )

        results.length shouldBe 9

        results.forall( _.documentId == docId1 ) shouldBe true
        results.forall( _.tenants.contains( tenant1 ) ) shouldBe true
        outputVersions.foreach( ov => results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        labelLists.foreach( ll => results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => results.exists( _.timestamp == ts ) )

        // tenant2 contains docId1 and docId3
        val results2 = readerSearch.search( ReaderOutputQuery( tenantId = Some( tenant2 ) ) ).await
        results2.length shouldBe 18

        results.forall( v => v.documentId == docId1 || v.documentId == docId3 ) shouldBe true
        results.forall( _.tenants.exists( _.contains( tenant2 ) ) ) shouldBe true
        outputVersions.foreach( ov => results.exists( _.outputVersion.contains( ov ) ) shouldBe true )
        labelLists.foreach( ll => results.exists( _.labels.toSet == ll.toSet ) shouldBe true )
        tss.foreach( ts => results.exists( _.timestamp == ts ) )
    }

    behavior of "query by timestamp"

    it should "return only records whose timestamps are later than date/time provided in 'after'" in {
        val results = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( after = ts1 ) ) ) ).await

        results.length shouldBe 18
        results.forall( r => r.timestamp.toEpochSecond( ZoneOffset.UTC ) > ts1.toEpochSecond( ZoneOffset.UTC ) )

        val results2 = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( after = ts2 ) ) ) ).await

        results2.length shouldBe 9
        results2.forall( r => r.timestamp.toEpochSecond( ZoneOffset.UTC ) > ts2.toEpochSecond( ZoneOffset.UTC ) )

        val results3 = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( after = ts3 ) ) ) ).await

        results3.length shouldBe 0
    }

    it should "return only records whose timestamps are earlier than date/time provided in 'before'" in {
        val results = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( before = ts3 ) ) ) ).await

        results.length shouldBe 18
        results.forall( r => r.timestamp.toEpochSecond( ZoneOffset.UTC ) < ts3.toEpochSecond( ZoneOffset.UTC ) )

        val results2 = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( before = ts2 ) ) ) ).await

        results2.length shouldBe 9
        results2.forall( r => r.timestamp.toEpochSecond( ZoneOffset.UTC ) < ts2.toEpochSecond( ZoneOffset.UTC ) )

        val results3 = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( before = ts1 ) ) ) ).await

        results3.length shouldBe 0
    }

    it should "return only records whose timestamps are exactly the date/time provided in 'on'" in {
        val results = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( on = ts1 ) ) ) ).await

        results.length shouldBe 9
        results.forall( r => r.timestamp.toEpochSecond( ZoneOffset.UTC ) == ts1.toEpochSecond( ZoneOffset.UTC ) )

        val results2 = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( on = ts2 ) ) ) ).await

        results2.length shouldBe 9
        results2.forall( r => r.timestamp.toEpochSecond( ZoneOffset.UTC ) == ts2.toEpochSecond( ZoneOffset.UTC ) )

        val results3 = readerSearch.search( ReaderOutputQuery( timestamp = Some( TimestampQuery( on = ts3 ) ) ) ).await

        results3.length shouldBe 9
        results2.forall( r => r.timestamp.toEpochSecond( ZoneOffset.UTC ) == ts3.toEpochSecond( ZoneOffset.UTC ) )
    }

    behavior of "query by multiple fields"

    it should "be able to search with all fields at the same time" in {
        val results = readerSearch.search(
            ReaderOutputQuery(
                readers = Some( List( readerIdentity1, readerIdentity2 ) ),
                versions = Some( List( readerVersion2, readerVersion3 ) ),
                documentIds = Some( List( docId1, docId3 ) ),
                outputVersions = Some( List( outputVersion1, outputVersion2 ) ),
                labels = Some( List( label1 ) ),
                tenantId = Some( tenant1 ),
                timestamp = Some( TimestampQuery( after = ts1 ) ) )
            ).await

        results.nonEmpty
        results.forall( r => r.timestamp.toEpochSecond( ZoneOffset.UTC ) > ts1.toEpochSecond( ZoneOffset.UTC ) ) shouldBe true
        results.forall( r => r.tenants.exists( _.contains( tenant1 ) ) ) shouldBe true
        results.forall( r => r.labels.exists( _.contains( label1 ) ) ) shouldBe true
        results.forall( r => r.outputVersion.contains( outputVersion1 ) || r.outputVersion.contains( outputVersion2 ) ) shouldBe true
        results.forall( r => r.documentId == docId1 || r.documentId == docId3 ) shouldBe true
        results.forall( r => r.version == readerVersion2 || r.version == readerVersion3 ) shouldBe true
        results.forall( r => r.identity == readerIdentity1 || r.identity == readerIdentity2 ) shouldBe true
    }

    behavior of "ReaderSearchService.exists"

    it should "return Future( true ) if storage key exists" in {
        val storageKeyExists = readerSearch.exists( recordSet1.head.storageKey ).await

        assert( storageKeyExists )
    }

    it should "return Future( false ) if storage key does not exist" in {
        val storageKeyExists = readerSearch.exists( "non-existent-key" ).await

        assert( !storageKeyExists )
    }
}

object ReaderSearchTestData {

    val readerIdentity1 = "test-reader-1"
    val readerIdentity2 = "test-reader-2"
    val readerIdentity3 = "test-reader-3"
    val readerIdentities : Array[ String ] = Array( readerIdentity1, readerIdentity2, readerIdentity3 )

    val readerVersion1 = "1.0.0"
    val readerVersion2 = "1.0.2"
    val readerVersion3 = "2.3.1"
    val readerVersions : Array[ String ] = Array( readerVersion1, readerVersion2, readerVersion3 )

    val docId1 = "test-doc-id-1"
    val docId2 = "test-doc-id-2"
    val docId3 = "test-doc-id-3"
    val docIds : Array[ String ] = Array( docId1, docId2, docId3 )

    val outputVersion1 = "test-output-version-1"
    val outputVersion2 = "test-output-version-2"
    val outputVersion3 = "test-output-version-3"
    val outputVersions : Array[ String ] = Array( outputVersion1, outputVersion2, outputVersion3 )

    val tenant1 = "test-tenant-1"
    val tenant2 = "test-tenant-2"
    val tenant3 = "test-tenant-3"
    val tenants : Array[ String ] = Array( tenant1, tenant2, tenant3 )

    val tenantList1 = Seq( tenant1, tenant2 )
    val tenantList2 : Seq[ String ] = Nil
    val tenantList3 = Seq( tenant2, tenant3 )

    val tenantMap = Map( docId1 -> tenantList1, docId2 -> tenantList2, docId3 -> tenantList3 )

    val label1 = "test-label-1"
    val label2 = "test-label-2"
    val label3 = "test-label-3"

    val labelList1 = Seq( label1, label2 )
    val labelList2 : Seq[ String ] = Nil
    val labelList3 = Seq( label2, label3 )

    val labelLists : Array[ Seq[ String ] ] = Array( labelList1, labelList2, labelList3 )

    val ts1 : LocalDateTime = LocalDateTime.of( 2000, 1, 1, 1, 1, 1 )
    val ts2 : LocalDateTime = LocalDateTime.of( 2000, 1, 1, 2, 1, 1 )
    val ts3 : LocalDateTime = LocalDateTime.of( 2000, 1, 1, 3, 1, 1 )
    val tss : Array[ LocalDateTime ] = Array( ts1, ts2, ts3 )

    import java.time.format.DateTimeFormatter

    def testStorageKey( id : Int, version : Int, docId : Int, outVersion : Int, labell : Int, ts : Int ) : String = {
        val rid = readerIdentities( id )
        val rv = readerVersions( version )
        val did = docIds( docId )
        val ov = ( if ( outVersion >= 0 && outVersion < 3 ) Some( outputVersions( outVersion ) ) else None ).mkString( "" )
        val l = Some( labelLists( labell ).mkString( "-" ) ).mkString( "" )
        val tsp = tss( ts ).format( DateTimeFormatter.ofPattern( "yyyy-MM-dd-HH-mm-ss" ) )
        s"$rid:$rv:$did:$ov:$l:$tsp"
    }

    def record( id : Int, version : Int, docId : Int, outVersion : Int, labell : Int, ts : Int ) : ReaderOutputMetadataRecord = {
        val rid = readerIdentities( id )
        val rv = readerVersions( version )
        val did = docIds( docId )
        val ov = if ( outVersion >= 0 && outVersion < 3 ) Some( outputVersions( outVersion ) ) else None
        val l = labelLists( labell ).toList
        val tsp = tss( ts )
        val sk = testStorageKey( id, version, docId, outVersion, labell, ts )
        ReaderOutputMetadataRecord( rid, rv, did, sk, ov, l, tsp )
    }


    def getRecordSet( i : Int ) : Seq[ ReaderOutputMetadataRecord ] = for {
        id <- 0 to 2
        did <- 0 to 2
    } yield record( id, i, did, i, i, i )

    val recordSet1 : Seq[ ReaderOutputMetadataRecord ] = getRecordSet( 0 )
    val recordSet2 : Seq[ ReaderOutputMetadataRecord ] = getRecordSet( 1 )
    val recordSet3 : Seq[ ReaderOutputMetadataRecord ] = getRecordSet( 2 )

    val allRecords : Seq[ ReaderOutputMetadataRecord ] = recordSet1 ++ recordSet2 ++ recordSet3
}
