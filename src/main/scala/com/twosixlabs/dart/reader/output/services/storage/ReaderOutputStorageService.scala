package com.twosixlabs.dart.reader.output.services.storage

import java.util.UUID
import scala.concurrent.Future

trait ReaderOutputStorageService {

    def upload( storageKey : String, fileContent : Array[ Byte ] ) : Future[ String ]

    def retrieve( storageKey : String ) : Future[ Array[ Byte ] ]

    def getUUID : String = {
        UUID.randomUUID().toString
    }
}
