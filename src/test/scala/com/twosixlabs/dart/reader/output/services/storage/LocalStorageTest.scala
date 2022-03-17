package com.twosixlabs.dart.reader.output.services.storage

import better.files.File
import com.twosixlabs.dart.reader.output.services.storage.LocalStorageTest.storageDir
import com.twosixlabs.dart.test.utilities.TestUtils
import com.twosixlabs.dart.test.utilities.TestUtils.FutureImplicits
import org.scalamock.scalatest.MockFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object LocalStorageTest {
    val config = TestUtils.getConfig
    val storageDir : String = config.getString( "persistence.dir" )
    File( storageDir ).createIfNotExists( asDirectory = true )

    import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors._

    val storageService : LocalReaderOutputStorageService = config.build[ LocalReaderOutputStorageService ]
}

class LocalStorageTest extends ReaderOutputStorageServiceTest( LocalStorageTest.storageService ) with MockFactory {

    val futureImplicits = new FutureImplicits( 5000, 250 )

    import futureImplicits._

    override def removeFile( key : String ) : Future[ Unit ] = {
        Future( File( storageDir ) / key delete ( true ) ).map( _ => () )
    }

    class TestReaderOutputStorageService( persistenceDirPath : String ) extends LocalReaderOutputStorageService( LocalReaderOutputStorageService.Dependencies( persistenceDirPath : String ) ) {
        override def getUUID : String = {
            "qwerty"
        }
    }

    "LocalStorage" should "save an uploaded file" in {
        File.usingTemporaryDirectory() { tempDir =>
            val storage = new TestReaderOutputStorageService( tempDir.canonicalPath )
            val testFile : File = File( "src/test/resources/reader_output/test.json" )
            val expectedFile : File = File( tempDir.canonicalPath + "/qwerty.json" )

            val f = storage.upload( testFile.canonicalPath, testFile.loadBytes ).await

            f shouldBe "qwerty.json"
            expectedFile.exists shouldBe true
            testFile.contentAsString shouldBe expectedFile.contentAsString
        }
    }

    "LocalStorage" should "fail when persist directory does not exist" in {
        val storage = new TestReaderOutputStorageService( "DOESNOTEXIST" )
        val storageError = "Unable To save qwerty.json to local disk"
        val testFile : File = File( "src/test/resources/reader_output/test.json" )
        storage.upload( testFile.canonicalPath, testFile.loadBytes ).awaitTry match {
            case Success( _ ) => fail
            case Failure( e ) =>
                e.getMessage shouldBe storageError
        }
    }

    "LocalStorage" should "successfully return file content" in {
        File.usingTemporaryDirectory() { tempDir =>
            val storage = new TestReaderOutputStorageService( tempDir.canonicalPath )
            val testFile : File = File( "src/test/resources/reader_output/test.json" )
            val expectedFile : File = File( tempDir.canonicalPath + "/qwerty.json" )
            storage.upload( testFile.canonicalPath, testFile.loadBytes ).awaitTry match {
                case Success( f ) =>
                    f shouldBe "qwerty.json"
                    expectedFile.exists shouldBe true
                    testFile.contentAsString shouldBe expectedFile.contentAsString
                case Failure( _ ) => fail
            }

            storage.retrieve( "qwerty.json" ).awaitTry match {
                case Success( storedFile ) => storedFile shouldBe expectedFile.loadBytes
                case Failure( _ ) => fail
            }
        }
    }

    "LocalStorage" should "fail to return unknown file" in {
        File.usingTemporaryDirectory() { tempDir =>
            val storage = new TestReaderOutputStorageService( tempDir.canonicalPath )
            val storageError = "Unable To retrieve qwerty.json from storage"
            storage.retrieve( "qwerty.json" ).awaitTry match {
                case Success( _ ) => fail
                case Failure( e ) =>
                    e.getMessage shouldBe storageError
            }
        }
    }

}
