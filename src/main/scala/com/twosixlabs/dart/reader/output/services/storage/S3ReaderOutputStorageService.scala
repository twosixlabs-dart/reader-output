package com.twosixlabs.dart.reader.output.services.storage

import com.twosixlabs.dart.aws.S3Bucket
import com.twosixlabs.dart.reader.output.exceptions.UnableToRetrieveDocumentException
import com.twosixlabs.dart.exceptions.ServiceUnreachableException
import com.twosixlabs.dart.utils.RetryHelper
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.awscore.exception.AwsServiceException

import scala.concurrent.Future
import scala.util.{Failure, Success}

class S3ReaderOutputStorageService( dependencies : S3ReaderOutputStorageService.Dependencies ) extends ReaderOutputStorageService {

    import dependencies._

    private val LOG : Logger = LoggerFactory.getLogger( getClass )
    private final val NUMBER_OF_RETRIES = 3

    override def upload( fileName : String,
                         fileContent : Array[ Byte ] ) : Future[ String ] = Future.fromTry {
        val extension = fileName.split( "\\." ).last
        val uuidFileName = s"${getUUID}.${extension}"

        RetryHelper.retry( NUMBER_OF_RETRIES )( s3Bucket.create( uuidFileName, fileContent ) ) match {
            case Success( _ ) => Success( uuidFileName )
            case Failure( e ) =>
                logError( e )
                Failure( e )
        }
    }

    override def retrieve( storageKey : String ) : Future[ Array[ Byte ] ] = Future.fromTry {
        RetryHelper.retry( NUMBER_OF_RETRIES )( s3Bucket.get( storageKey ) ) match {
            case Success( Some( content ) ) => Success( content )
            case Success( None ) => Failure( new UnableToRetrieveDocumentException( s"${storageKey} does not exist" ) )
            case Failure( e ) =>
                logError( e )
                e match {
                    case _ : AwsServiceException => Failure( new UnableToRetrieveDocumentException( storageKey ) )
                    case _ => Failure( new ServiceUnreachableException( "storage" ) )
                }
        }
    }

    private def logError( e : Throwable ) : Unit = {
        LOG.error(
            s"""${e.getClass}: ${e.getMessage}
               |${e.getCause}
               |${e.getStackTrace.mkString( "\n" )
            }""".stripMargin )
    }
}

object S3ReaderOutputStorageService {
    case class Dependencies( s3Bucket: S3Bucket ) {
        def build() : S3ReaderOutputStorageService = new S3ReaderOutputStorageService( this )
    }

    def apply( s3Bucket : S3Bucket ) : S3ReaderOutputStorageService =
        Dependencies( s3Bucket ).build()
}
