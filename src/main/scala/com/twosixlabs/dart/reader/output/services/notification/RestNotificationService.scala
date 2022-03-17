package com.twosixlabs.dart.reader.output.services.notification

import com.twosixlabs.dart.reader.output.models.ReaderModels.{ReaderOutputMetadataSubmission, ReaderOutputSubmissionResult}
import com.twosixlabs.dart.json.JsonFormat.marshalFrom
import okhttp3.{MediaType, OkHttpClient, Request, RequestBody, Response}
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.TimeUnit

class RestNotificationService( dependencies : RestNotificationService.Dependencies ) extends NotificationService {

    import dependencies._

    private val LOG : Logger = LoggerFactory.getLogger( getClass )
    private val JSON : MediaType = MediaType.get( "application/json; charset=utf-8" )

    private val client : OkHttpClient = new OkHttpClient.Builder()
                                              .connectTimeout( restTimeoutMs, TimeUnit.MILLISECONDS )
                                              .callTimeout( restTimeoutMs, TimeUnit.MILLISECONDS )
                                              .readTimeout( restTimeoutMs, TimeUnit.MILLISECONDS )
                                              .writeTimeout( restTimeoutMs, TimeUnit.MILLISECONDS )
                                              .build

    override def notify( attempt : Either[ (Throwable, ReaderOutputMetadataSubmission) ,ReaderOutputSubmissionResult ] ) : Unit  = attempt match {
        case Left( _ ) =>
        case Right( value ) =>
            val resultJson = marshalFrom( value ).get
            try {
                val body = RequestBody.create( resultJson, JSON )
                val request : Request = new Request.Builder().url( url ).post( body ).build
                val response : Response = client.newCall( request ).execute
                response.code match {
                    case 200 => {
                        LOG.info( s"REST message ${value.documentId} successfully sent to ${url} with response ${response.body.string}" )
                    }
                    case _ => {
                        LOG.error( s"Error sending notification to ${url}" )
                        LOG.error( s"${response.code} :: ${response.body.string}" )
                    }
                }
            } catch {
                case e : Throwable =>
                    LOG.error( s"Could not notify ${url}" )
                    LOG.error( e.getMessage )
            }
    }
}

object RestNotificationService {
    case class Dependencies( url : String,
                             restTimeoutMs : Long = 3000 ) {
        def build() : RestNotificationService = new RestNotificationService( this )
    }

    def apply( url : String,
               restTimeoutMs : Long = 3000 ) : RestNotificationService =
        Dependencies( url, restTimeoutMs ).build()
}
