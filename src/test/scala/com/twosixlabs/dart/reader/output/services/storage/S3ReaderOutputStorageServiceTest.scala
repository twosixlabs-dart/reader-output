package com.twosixlabs.dart.reader.output.services.storage

import better.files.File
import com.twosixlabs.dart.test.utilities.TestUtils
import io.findify.s3mock.S3Mock
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Future

object S3ReaderOutputStorageServiceTest {
    private val config = TestUtils.getConfig
    val dataDir : String = config.getString( "aws.test.data.dir" )
    val bucketName : String = config.getString( "persistence.bucket.name" )
    val mockPort : Int = config.getInt( "aws.test.port" )

    import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors._

    val readerStorage : S3ReaderOutputStorageService = config.build[ S3ReaderOutputStorageService ]
}

class S3ReaderOutputStorageServiceTest extends ReaderOutputStorageServiceTest( S3ReaderOutputStorageServiceTest.readerStorage ) with BeforeAndAfterAll {

    import com.twosixlabs.dart.reader.output.services.storage.S3ReaderOutputStorageServiceTest._

    val api : S3Mock = S3Mock( port = mockPort, dir = dataDir )

    override def beforeAll( ) : Unit = {
        super.beforeAll()
        ( File( dataDir ) / bucketName ).createIfNotExists( asDirectory = true )
        ( File( dataDir ) / bucketName ).clear()
        api.start
    }

    override def afterAll( ) : Unit = {
        api.shutdown
        ( File( dataDir ) / bucketName ).clear()
        super.afterAll()
    }

    override def removeFile( key : String ) : Future[ Unit ] = {
        import scala.concurrent.ExecutionContext.Implicits.global
        Future( ( File( dataDir ) / bucketName / key ).delete() ).map( _ => () )
    }

}
