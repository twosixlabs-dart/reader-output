package com.twosixlabs.dart.reader.output.services.metadata

import com.twosixlabs.dart.reader.output.models.ReaderModels.{ReaderOutputMetadataRecord, ReaderOutputMetadataSubmission}
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.test.utilities.TestUtils.FutureImplicits
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.Future

abstract class ReaderOutputMetadataServiceTest(
    readerService : ReaderOutputMetadataService,
) extends StandardTestBase3x with BeforeAndAfterEach with BeforeAndAfterAll {

    def clearRecords() : Future[ Unit ]

    def getAllRecords : Future[ Seq[ ReaderOutputMetadataRecord ] ]

    val futureImplicits = new FutureImplicits( 5000, 250 )
    import futureImplicits._

    override def beforeEach( ) : Unit = {
        clearRecords().awaitWrite
    }

    override def afterAll( ) : Unit = {
        beforeEach()
        super.afterAll()
    }

    behavior of "ReaderOutputMetadataService.submit"

    it should "Add a record to the datastore and return a correct confirmation result" in {
        val testRecord = ReaderOutputMetadataSubmission( "test-identity", "test-version", "test-doc-id", Some( "test-output-version" ), Some( Set( "test-label-1", "test-label-2" ) ) )
        val testKey = "test-storage-key"

        val res = readerService.submit( testRecord, testKey ).await

        res.identity shouldBe testRecord.identity
        res.version shouldBe testRecord.version
        res.documentId shouldBe testRecord.documentId
        res.outputVersion shouldBe testRecord.outputVersion
        res.storageKey shouldBe testKey
        res.labels shouldBe testRecord.labels

        val records = getAllRecords.await

        records.size shouldBe 1
        val storeRes = records.head

        storeRes.identity shouldBe testRecord.identity
        storeRes.version shouldBe testRecord.version
        storeRes.documentId shouldBe testRecord.documentId
        storeRes.outputVersion shouldBe testRecord.outputVersion
        storeRes.storageKey shouldBe testKey
        storeRes.labels.toSet shouldBe testRecord.labels.get
    }

    behavior of "ReaderOutputMetadataService.validateMetadata"

    it should "validate metadata if readerId, readerVersion, documentId, and outputVersion are not in single record in metadata store" in {
        val testRecord = ReaderOutputMetadataSubmission( "test-identity", "test-version", "test-doc-id", Some( "test-output-version" ), Some( Set( "test-label-1", "test-label-2" ) ) )

        readerService.validateMetadata( testRecord ).await shouldBe None
    }

    it should "not validate metadata if readerId, readerVersion, documentId, and outputVersion already exist in metadata store" in {
        val testRecord = ReaderOutputMetadataSubmission( "test-identity", "test-version", "test-doc-id", Some( "test-output-version" ), Some( Set( "test-label-1", "test-label-2" ) ) )
        val testKey = "test-storage-key"

        readerService.submit( testRecord, testKey ).awaitWrite

        readerService.validateMetadata( testRecord ).await.nonEmpty shouldBe true
        readerService.validateMetadata( testRecord.copy( labels = None ) ).await.nonEmpty shouldBe true
    }

}
