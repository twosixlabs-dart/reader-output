package com.twosixlabs.dart.reader.output.services.notification

import com.twosixlabs.dart.reader.output.models.ReaderModels
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputMetadataSubmission
import com.twosixlabs.dart.operations.status.PipelineStatus
import com.twosixlabs.dart.operations.status.PipelineStatus.{ProcessorType, Status}
import com.twosixlabs.dart.operations.status.client.PipelineStatusUpdateClient

class OperationsNotificationService( dependencies : OperationsNotificationService.Dependencies )
  extends NotificationService {

    import dependencies._

    override def notify( attempt : Either[ (Throwable, ReaderOutputMetadataSubmission), ReaderModels.ReaderOutputSubmissionResult ] ) : Unit = {
        val time = System.currentTimeMillis()
        attempt match {
            case Right( value ) =>
                opsClient.fireAndForget(
                    new PipelineStatus(
                        value.documentId,
                        value.identity,
                        ProcessorType.READER,
                        Status.SUCCESS,
                        "DART",
                        time,
                        time,
                        null,
                    )
                )

            case Left( (e, value) ) =>
                opsClient.fireAndForget(
                    new PipelineStatus(
                        value.documentId,
                        value.identity,
                        ProcessorType.READER,
                        Status.FAILURE,
                        "DART",
                        time,
                        time,
                        s"${e.getClass}: ${e.getMessage}",
                    )
                )
        }
    }
}

object OperationsNotificationService {
    case class Dependencies( opsClient : PipelineStatusUpdateClient ) {
        def build() : OperationsNotificationService = new OperationsNotificationService( this )
    }

    def apply( opsClient : PipelineStatusUpdateClient ) : OperationsNotificationService =
        Dependencies( opsClient ).build()
}
