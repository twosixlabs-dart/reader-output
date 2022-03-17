package com.twosixlabs.dart.reader.output.services.metadata

import com.twosixlabs.dart.reader.output.models.ReaderModels.{ReaderOutputMetadataSubmission, ReaderOutputSubmissionResult}

import scala.concurrent.Future

trait ReaderOutputMetadataService {

    def submit( metadata : ReaderOutputMetadataSubmission, storageKey : String ) : Future[ ReaderOutputSubmissionResult ]

    /**
     * Return nothing if valid, and if invalid, return message explaining why.
     * Main reason for invalidity is if metadata is duplicate of preexisting outputs
     * @param metadata
     * @return
     */
    def validateMetadata( metadata : ReaderOutputMetadataSubmission ) : Future[ Option[ String ] ]

}
