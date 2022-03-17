package com.twosixlabs.dart.reader.output.services.notification

import com.twosixlabs.dart.reader.output.models.ReaderModels.{ReaderOutputMetadataSubmission, ReaderOutputSubmissionResult}

trait NotificationService {

    /**
     * Notifies some listener of *either* a failure to upload *or* the results of an upload.
     * In the case of failure, it is passed both the exception and metadata of the original
     * submission
     * @param attempt
     */
    def notify( attempt : Either[ (Throwable, ReaderOutputMetadataSubmission), ReaderOutputSubmissionResult ] ) : Unit

}
