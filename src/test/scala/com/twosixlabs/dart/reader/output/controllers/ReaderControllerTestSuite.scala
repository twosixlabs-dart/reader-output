package com.twosixlabs.dart.reader.output.controllers

import better.files.File
import com.twosixlabs.dart.auth.controllers.SecureDartController.AuthDependencies
import com.twosixlabs.dart.auth.groups.ProgramManager
import com.twosixlabs.dart.auth.user.DartUser
import com.twosixlabs.dart.reader.output.configuration.ConfigConstructors.{AuthFromConfig, FromConfig}
import com.twosixlabs.dart.reader.output.exceptions.InvalidMetadataException
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputSubmissionResult
import com.twosixlabs.dart.reader.output.services.submission.{DartReaderOutputSubmissionService, ReaderOutputSubmissionService}
import com.twosixlabs.dart.operations.status.client.PipelineStatusUpdateClient
import com.twosixlabs.dart.test.base.StandardTestBase3x
import com.twosixlabs.dart.test.utilities.TestUtils
import org.scalatra.test.scalatest.ScalatraSuite
import org.slf4j.{Logger, LoggerFactory}

import javax.servlet.http.HttpServletRequest
import scala.concurrent.Future

class ReaderControllerTestSuite extends StandardTestBase3x with ScalatraSuite {

    private val LOG : Logger = LoggerFactory.getLogger( getClass )

    val config = TestUtils.getConfig

    val authDeps = config.build[ AuthDependencies ]

    private val streamFileThreshold = config.getInt( "file.stream.threshold.mb" )

    private val opsClient = mock[ PipelineStatusUpdateClient ]
    private val readerService = mock[ DartReaderOutputSubmissionService ]

    private val readerControllerDeps = ReaderController.Dependencies( readerService,
                                                                      streamFileThreshold,
                                                                      opsClient,
                                                                      authDeps )

    private val readerController = new ReaderController( readerControllerDeps ) {
        override def authenticateUser( req : HttpServletRequest ) : DartUser =
            DartUser( "test-user", Set( ProgramManager ) )
    }

    addServlet( readerController, "/*" )

    val endpoint = "/"

    val testFile : File = File( "src/test/resources/reader_output/test.json" )

    val testIdentity = "testReader"
    val testVersion = "1.3.3.7"
    val testDocumentId = "d2bc6f81cb3f2c929ed8aac7cb1e3873"
    val testStorageKey = "123321.txt"
    val outputVersion = "1.2.1"
    val testLabel1 = "test-label-1"
    val testLabel2 = "test-label-2"
    val testLabels = List( testLabel1, testLabel2 )

    private val submissionResult = ReaderOutputSubmissionResult(
        testIdentity,
        testVersion,
        testDocumentId,
        testStorageKey,
        Some( outputVersion ),
        Some( testLabels.toSet ),
    )

    private val mockedMetaData =
        s"""{
           | "identity": "${testIdentity}",
           | "version": "${testVersion}",
           | "document_id": "${testDocumentId}",
           | "output_version": "${outputVersion}",
           | "labels": ["${testLabel1}", "${testLabel2}"]
           |}
           |""".stripMargin

    "POST to /upload" should "return 201 on successful upload" in {
        when( readerService.submit( *, *, * ) ).thenReturn( Future.successful( submissionResult ) )
        doNothing.when( opsClient ).fireAndForget( * )

        val expectedResponse = s"""{"identity":"${testIdentity}","version":"${testVersion}","document_id":"${testDocumentId}","storage_key":"${testStorageKey}","output_version":"${outputVersion}","labels":["${testLabel1}","${testLabel2}"]}"""
        submitMultipart( method = "POST", path = endpoint, params = Array( ("metadata", mockedMetaData) ), files = Array( ("file", testFile.toJava) ) ) {
            // check scalatra processing
            status shouldBe 201
            body shouldBe expectedResponse
        }
    }

    private val mockedMetaDataWithOnlyRequiredFields =
        s"""{
           | "identity": "${testIdentity}",
           | "version": "${testVersion}",
           | "document_id": "${testDocumentId}",
           | "output_version": "${outputVersion}"
           |}
           |""".stripMargin

    "POST to /upload" should "return 400 when reader service returns an invalid metadata exception" in {
        when( readerService.submit( *, *, * ) ).thenReturn( Future.failed( new InvalidMetadataException( "test-reason" ) ) )
        doNothing.when( opsClient ).fireAndForget( * )

        submitMultipart( method = "POST", path = endpoint, params = Array( ("metadata", mockedMetaDataWithOnlyRequiredFields) ), files = Array( ("file", testFile.toJava) ) ) {
            // check scalatra processing
            status shouldBe 400
            body should include( "invalid metadata: test-reason" )
        }
    }

    "POST to /upload" should "return 400 on missing metadata" in {
        val expectedResponse = s"""{"status":400,"error_message":"Bad request: invalid request body: no metadata provided"}"""

        //@formatter:off
        submitMultipart( method = "POST", path = endpoint, files = Array( ("file", testFile.toJava) ) ) {
            status shouldBe 400
            body shouldBe expectedResponse
        }
        //@formatter:on
    }

    "POST to /upload" should "return 400 on missing file" in {
        val expectedResponse = s"""{"status":400,"error_message":"Bad request: invalid request body: no file provided"}"""

        //@formatter:off
        submitMultipart( method = "POST", path = endpoint, params = Array( ("metadata", mockedMetaData) ) ) {
            status shouldBe 400
            body shouldBe expectedResponse
        }
        //@formatter:on
    }

    "POST to /upload" should "return 400 when reader output metadata is invalid" in {
        val testMessage = "test-message"
        val expectedResponse = s"""{"status":400,"error_message":"Bad request: invalid request body: invalid metadata: $testMessage"}"""

        when( readerService.submit( *, *, * ) ).thenReturn( Future.failed( new InvalidMetadataException( testMessage ) ) )

        submitMultipart( method = "POST", path = endpoint, params = Array( ("metadata", mockedMetaData) ), files = Array( ("file", testFile.toJava) ) ) {
            status shouldBe 400
            body shouldBe expectedResponse
        }
    }

    override def header = null
}
