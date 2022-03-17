package com.twosixlabs.dart.reader.output.services.query

import com.twosixlabs.dart.reader.output.models.QueryModels.{ReaderOutputMetadataQueryResult, ReaderOutputQuery}

import scala.concurrent.Future

trait ReaderSearchService {

    def search( query : ReaderOutputQuery ) : Future[ Seq[ ReaderOutputMetadataQueryResult ] ]

    def exists( storageKey : String ) : Future[ Boolean ]

}
