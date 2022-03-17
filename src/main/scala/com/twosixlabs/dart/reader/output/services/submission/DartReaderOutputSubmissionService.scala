package com.twosixlabs.dart.reader.output.services.submission

import com.twosixlabs.dart.reader.output.models.ReaderModels
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputMetadataSubmission
import com.twosixlabs.dart.reader.output.services.metadata.ReaderOutputMetadataService
import com.twosixlabs.dart.reader.output.services.notification.NotificationService
import com.twosixlabs.dart.reader.output.services.storage.ReaderOutputStorageService
import com.twosixlabs.dart.exceptions.ExceptionImplicits.FutureExceptionLogging

import scala.concurrent.{ExecutionContextExecutor, Future}

class DartReaderOutputSubmissionService(
    dependencies : DartReaderOutputSubmissionService.Dependencies
) extends ReaderOutputSubmissionService {
    override implicit val executionContext : ExecutionContextExecutor = dependencies.executionContext

    override def handleMetadata(
        outputSubmission : ReaderModels.ReaderOutputMetadataSubmission,
        storageKey : String
    ) : Future[ ReaderModels.ReaderOutputSubmissionResult ] =
        dependencies.metadataService.submit( outputSubmission, storageKey )

    override def handleData( filename : String, fileContent : Array[ Byte ] ) : Future[ String ] = {
        dependencies.storageService.upload( filename, fileContent )
    }

    def notify( attempt : Either[ (Throwable, ReaderOutputMetadataSubmission), ReaderModels.ReaderOutputSubmissionResult ] ) : Future[ Unit ] = {
        Future.sequence {
            dependencies.notificationServices.map( service => Future( service.notify( attempt ) ) )
        } map { _ => () } logged
    }

    /**
     * Return empty if valid, and message if invalid. Message describes why invalid.
     *
     * @param outputSubmission
     * @return
     */
    override def validateMetadata( outputSubmission: ReaderOutputMetadataSubmission ): Future[ Option[ String ] ] = {
        dependencies.metadataService.validateMetadata( outputSubmission )
    }
}

object DartReaderOutputSubmissionService {
    case class Dependencies( storageService : ReaderOutputStorageService,
                             metadataService : ReaderOutputMetadataService,
                             notificationServices : Seq[ NotificationService ],
                             executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) {
        def build() : DartReaderOutputSubmissionService = new DartReaderOutputSubmissionService( this )
    }

    def apply( storageService : ReaderOutputStorageService,
               metadataService : ReaderOutputMetadataService,
               notificationServices : Seq[ NotificationService ],
               executionContext: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) : DartReaderOutputSubmissionService =
        Dependencies( storageService, metadataService, notificationServices, executionContext ).build()
}
