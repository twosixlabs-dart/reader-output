package com.twosixlabs.dart.reader.output.services.submission

import com.twosixlabs.dart.reader.output.exceptions.InvalidMetadataException
import com.twosixlabs.dart.reader.output.models.ReaderModels.{ReaderOutputMetadataSubmission, ReaderOutputSubmissionResult}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

trait ReaderOutputSubmissionService {

    implicit val executionContext : ExecutionContextExecutor

    /**
     * Return empty if valid, and message if invalid. Message describes why invalid.
     * @param outputSubmission
     * @return
     */
    def validateMetadata( outputSubmission : ReaderOutputMetadataSubmission ) : Future[ Option[ String ] ]

    def handleMetadata( outputSubmission : ReaderOutputMetadataSubmission, storageKey : String ) : Future[ ReaderOutputSubmissionResult ]

    def handleData( filename : String, fileContent : Array[ Byte ] ) : Future[ String ]

    def notify( attempt : Either[ (Throwable, ReaderOutputMetadataSubmission), ReaderOutputSubmissionResult ] ) : Future[ Unit ]

    def submit( filename : String, fileContent : Array[ Byte ], metadata : ReaderOutputMetadataSubmission ) : Future[ ReaderOutputSubmissionResult ] = {
        for {
            _ <- validateMetadata( metadata ) transform {
                case Success( None ) => Success()
                case Success( Some( whyInvalid ) ) => Failure( new InvalidMetadataException( whyInvalid ) )
                case Failure( e ) => Failure( new InvalidMetadataException( s"unknown validation failure: ${e.getClass}, ${e.getMessage}" ) )
            }
            storageKey <- handleData( filename, fileContent )
            result <- handleMetadata( metadata, storageKey ) transformWith { (tryResult : Try[ ReaderOutputSubmissionResult ]) => {
                notify( tryResult match {
                    case Success( res ) => Right( res )
                    case Failure( e ) => Left( e, metadata )
                } ) transform { _ => tryResult }
            } }
        } yield result
    }

}

