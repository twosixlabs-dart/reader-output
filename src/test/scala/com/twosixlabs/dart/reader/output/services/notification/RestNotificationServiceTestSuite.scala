package com.twosixlabs.dart.reader.output.services.notification

import com.twosixlabs.dart.commons.config.StandardCliConfig
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputSubmissionResult
import com.twosixlabs.dart.test.base.StandardTestBase3x
import okhttp3.mockwebserver.{MockResponse, MockWebServer}

import scala.util.Random

class RestNotificationServiceTestSuite extends StandardTestBase3x with StandardCliConfig {

    private val PORT_LOWER_BOUND : Int = 8000
    private val PORT_UPPER_BOUND : Int = 9999

    val uploadResult : ReaderOutputSubmissionResult = ReaderOutputSubmissionResult( "testreader", "1.2.3", "doc.id", "storage.key", Some( "output_version" ), Some( Set( "label" ) ) )

    // TODO: dumb test, it should do more... ???
    "RestNotification" should "be able to send a notification and receive a 200" in {
        val port : Int = randomHttpPort()

        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 200 )
        mockServer.enqueue( mockResponse )
        mockServer.start( port )

        val url = f"http://${mockServer.getHostName}:${mockServer.getPort}"

        val notifier = RestNotificationService( url )
        notifier.notify( Right( uploadResult ) )

        mockServer.shutdown()
    }

    // TODO: dumb test, it should do more... ???
    "RestNotification" should "be able to handle a failed notification and move on gracefully" in {
        val port : Int = randomHttpPort()

        val mockServer : MockWebServer = new MockWebServer()
        val mockResponse = new MockResponse()
          .setResponseCode( 503 )
        mockServer.enqueue( mockResponse )
        mockServer.start( port )

        val url = f"http://${mockServer.getHostName}:${mockServer.getPort}"

        val notifier = RestNotificationService( url )
        notifier.notify( Right( uploadResult ) )

        mockServer.shutdown()
    }

    private def randomHttpPort( ) : Int = {
        PORT_LOWER_BOUND + Random.nextInt( ( PORT_UPPER_BOUND - PORT_LOWER_BOUND ) + 1 )
    }

}
