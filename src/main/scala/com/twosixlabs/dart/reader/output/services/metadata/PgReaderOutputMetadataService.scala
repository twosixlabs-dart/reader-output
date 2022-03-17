package com.twosixlabs.dart.reader.output.services.metadata
import com.twosixlabs.dart.reader.output.models.ReaderModels
import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputMetadataSubmission
import com.twosixlabs.dart.reader.output.models.tables.Schema

import scala.concurrent.{ExecutionContextExecutor, Future}

import com.twosixlabs.dart.reader.output.configuration.PgSlickProfile.api._

object PgReaderOutputMetadataService {
    case class Dependencies( db : Database,
                             executionContext : ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) {
        def build() : PgReaderOutputMetadataService = new PgReaderOutputMetadataService( this )
    }

    def apply( db : Database,
               executionContext : ExecutionContextExecutor = scala.concurrent.ExecutionContext.global ) : PgReaderOutputMetadataService =
        Dependencies( db, executionContext ).build()
}

class PgReaderOutputMetadataService( dependencies : PgReaderOutputMetadataService.Dependencies )
  extends ReaderOutputMetadataService {
    import dependencies._

    implicit private val ec : ExecutionContextExecutor = executionContext

    override def submit( metadata : ReaderOutputMetadataSubmission,
                         storageKey : String ) : Future[ ReaderModels.ReaderOutputSubmissionResult ] = {
        db.run(
            Schema
              .readerOutputTableInsert
              .returning( Schema.readerOutputTableQuery )
              .+=( metadata.toTuple( storageKey ) )
        ) map ( _.toSubmissionResult )
    }

    /**
     * Return nothing if valid, and if invalid, return message explaining why.
     * Main reason for invalidity is if metadata is duplicate of preexisting outputs
     *
     * @param metadata
     * @return
     */
    override def validateMetadata( metadata: ReaderOutputMetadataSubmission ) : Future[ Option[ String ] ] = {
        db.run(
            Schema
              .readerOutputTableQuery
              .filter( _.readerId === metadata.identity )
              .filter( _.readerVersion === metadata.version )
              .filter( _.documentId === metadata.documentId )
              .filter( _.outputVersion === metadata.outputVersion )
              .exists
              .result
        ) map {
            case false => None
            case true => Some( s"reader output already exists for reader ${metadata.identity}, version ${metadata.version}, document ${metadata.documentId}, and output version ${metadata.outputVersion}" )
        }
    }
}
