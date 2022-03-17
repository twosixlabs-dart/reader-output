package com.twosixlabs.dart.reader.output.services.storage

import better.files.File
import com.twosixlabs.dart.reader.output.exceptions.UnableToRetrieveDocumentException
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.test.utilities.TestUtils.FutureImplicits
import org.scalatest.BeforeAndAfterAll

import java.nio.charset.StandardCharsets
import scala.concurrent.Future
import scala.util.{Failure, Success}

abstract class ReaderOutputStorageServiceTest( storageService : ReaderOutputStorageService ) extends StandardTestBase3x with BeforeAndAfterAll {

    private val futureImplicits = new FutureImplicits( 5000, 250 )

    import futureImplicits._

    private val className = storageService.getClass.getSimpleName

    def removeFile( key : String ) : Future[ Unit ]

    behavior of s"${className}"

    it should "save an uploaded file" in {
        val file = File( "src/test/resources/reader_output/test.json" )
        val originalContents = file.contentAsString

        val key : String = storageService.upload( file.canonicalPath, file.loadBytes ).awaitWrite

        val retrievedFileData = storageService.retrieve( key ).await
        val retrievedContents = new String( retrievedFileData, StandardCharsets.UTF_8 )

        retrievedContents shouldBe originalContents

        removeFile( key ).awaitWrite
    }

    behavior of s"${className}::retrieve"

    it should "return an UnableToRetrieveDocumentException if document does not exist" in {
        storageService.retrieve( "non-existent-key" ).awaitTry match {
            case Success( _ ) => fail( "Successfully retrieved non existent file" )
            case Failure( e : UnableToRetrieveDocumentException ) => e.getMessage should include( "non-existent-key" )
            case Failure( e ) => fail( s"Threw wrong exception:\n${e.getClass.getName}: ${e.getMessage}" )
        }
    }

}
