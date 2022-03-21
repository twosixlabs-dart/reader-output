package com.twosixlabs.dart.reader.output.services.metadata

import com.twosixlabs.dart.reader.output.configuration.PgSlickProfile.api._
import com.twosixlabs.dart.reader.output.models.ReaderModels
import com.twosixlabs.dart.reader.output.models.tables.Schema
import com.twosixlabs.dart.test.utilities.TestUtils
import org.scalatest.Ignore

import scala.concurrent.Future

object PgReaderOutputMetadataServiceTest {
    val config = TestUtils.getConfig

    import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors._

    val db = config.build[ Database ]
    val readerService : PgReaderOutputMetadataService = PgReaderOutputMetadataService( db )
}

@Ignore
class PgReaderOutputMetadataServiceTest extends ReaderOutputMetadataServiceTest( PgReaderOutputMetadataServiceTest.readerService ) {

    override def clearRecords( ) : Future[ Unit ] = PgReaderOutputMetadataServiceTest.db.run {
        Schema.readerOutputTableQuery.schema.truncate
    }

    override def getAllRecords : Future[ Seq[ ReaderModels.ReaderOutputMetadataRecord ] ] = {
        PgReaderOutputMetadataServiceTest.db.run( Schema.readerOutputTableQuery.result )
    }

}
