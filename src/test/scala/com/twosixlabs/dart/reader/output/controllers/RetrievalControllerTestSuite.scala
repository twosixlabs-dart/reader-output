package com.twosixlabs.dart.reader.output.controllers

import better.files.File
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.auth.groups.ProgramManager
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors.{AuthFromConfig, FromConfig}
import com.twosixlabs.dart.reader.output.exceptions.UnableToRetrieveDocumentException
import com.twosixlabs.dart.reader.output.services.query.ReaderSearchService
import com.twosixlabs.dart.reader.output.services.storage.ReaderOutputStorageService
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.test.utilities.TestUtils
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{Logger, LoggerFactory}

import javax.servlet.http.HttpServletRequest
import scala.concurrent.Future

class RetrievalControllerTestSuite extends StandardTestBase3x with StandardCliConfig with BeforeAndAfterEach with ScalatraSuite with MockFactory {

    private val searchService = stub[ ReaderSearchService ]
    private val storageService = stub[ ReaderOutputStorageService ]

    private val config = TestUtils.getConfig

    private val authDeps = config.build[ AuthDependencies ]

    val retrievalControllerDeps =
        RetrievalController.Dependencies( searchService, storageService, authDeps )

    val retrievalController = new RetrievalController( retrievalControllerDeps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser =
            DartUser( "test-user", Set( ProgramManager ) )
    }

    addServlet( retrievalController, "/*" )

    val endpoint = "/"

    "GET from /download" should "return 200 and file content on successful plaintext retrieval" in {
        val testStorageKey : String = "test.json"
        val testFile : File = File( "src/test/resources/reader_output/test.json" )
        val testContent = testFile.loadBytes

        ( searchService.exists _ ).when( * ).returns( Future.successful( true ) )
        ( storageService.retrieve _ ).when( * ).returns( Future.successful( testContent ) )

        submitMultipart( method = "GET",
                         path = s"$endpoint/$testStorageKey" ) {
            // check scalatra processing
//            header should contain( "Content-Type" -> "application/octet-stream;charset=utf-8" )
            status shouldBe 200
            body shouldBe testFile.contentAsString
        }
    }

    "GET from /download" should "return 200 and file content on successful binary file retrieval" in {
        val testStorageKey : String = "test.jpg"
        val testFile : File = File( "src/test/resources/reader_output/test.jpg" )

        ( searchService.exists _ ).when( * ).returns( Future.successful( true ) )
        ( storageService.retrieve _ ).when( * )
          .returns( Future.successful( testFile.loadBytes ) )

        submitMultipart( method = "GET",
                         path = s"$endpoint/$testStorageKey" ) {
            // check scalatra processing
//            header should contain( "Content-Type" -> "application/octet-stream;charset=utf-8" )
            status shouldBe 200
            body shouldBe testFile.contentAsString
        }
    }

    "GET from /download" should "return 404 on file missing from database" in {
        ( searchService.exists _ ).when( * ).returns( Future.successful( false ) )

        submitMultipart( method = "GET",
                         path = "/NOPE" ) {
            // check scalatra processing
            status shouldBe 404
            body should ( include( "404" ) and include( "NOPE" ) )
        }
    }

    "GET from /download" should "return 500 on file missing from storage if key exists" in {
        ( searchService.exists _ ).when( * ).returns( Future.successful( true ) )
        ( storageService.retrieve _ ).when( * ).returns( Future.failed( new UnableToRetrieveDocumentException( "missing.txt" ) ) )

        submitMultipart( method = "GET",
                         path = "/missing.txt" ) {
            // check scalatra processing
            status shouldBe 500
        }
    }

//    "GET from /download" should "return 503 on failure to connect to storage" in {
//        ( dbs.blindCount _ ).when( * ).returns( Success( 1 ) )
//        ( storage.retrieve _ ).when( * ).returns( Failure( new ServiceUnreachableException( "storage" ) ) )
//
//        val expectedResponse = s"""{"status":503,"error_message":"Service unavailable: unable to reach storage"}"""
//
//        submitMultipart( method = "GET",
//                         path = "/NOPE" ) {
//            // check scalatra processing
//            status shouldBe 503
//            body shouldBe expectedResponse
//        }
//    }
    override def header = null
}
