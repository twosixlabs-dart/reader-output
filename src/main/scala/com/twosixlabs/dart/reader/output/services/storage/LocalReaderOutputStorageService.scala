package com.twosixlabs.dart.reader.output.services.storage

import better.files.File
import com.twosixlabs.dart.reader.output.exceptions.{UnableToCreateDocumentException, UnableToRetrieveDocumentException, UnableToSaveDocumentException}
import com.twosixlabs.dart.exceptions.ServiceUnreachableException
import com.twosixlabs.dart.utils.RetryHelper
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class LocalReaderOutputStorageService( dependencies : LocalReaderOutputStorageService.Dependencies )
  extends ReaderOutputStorageService {

    import dependencies._

    private val LOG : Logger = LoggerFactory.getLogger( getClass )
    private final val NUMBER_OF_RETRIES = 3

    override def upload( fileName : String,
                         fileContent : Array[ Byte ] ) : Future[ String ] = Future.fromTry {

        val extension = fileName.split( "\\." ).last
        val uuidFileName = s"${getUUID}.${extension}"

        Try {
            File( s"${persistenceDirPath}/${uuidFileName}" )
        } match {
            case Success( value ) =>
                val saveFile = () => Try { value.writeByteArray( fileContent ) } : Try[ File ]
                RetryHelper.retry( NUMBER_OF_RETRIES )( saveFile() ) match {
                    case Success( _ ) => Success( uuidFileName )
                    case Failure( e ) =>
                        logError( e )
                        Failure( new UnableToSaveDocumentException( uuidFileName ) )
                }
            case Failure( e ) =>
                logError( e )
                Failure( new UnableToCreateDocumentException( uuidFileName ) )
        }
    }

    override def retrieve( storageKey : String ) : Future[ Array[ Byte ] ] = Future.fromTry {
        Try {
            File( s"${persistenceDirPath}/${storageKey}" ).loadBytes
        } match {
            case Success( value ) => Success( value )
            case Failure( e ) =>
                logError( e )
                e match {
                    case _ : java.nio.file.NoSuchFileException => Failure( new UnableToRetrieveDocumentException( storageKey ) )
                    case _ => Failure( new ServiceUnreachableException( "storage" ) )
                }
        }
    }

    private def logError( e : Throwable ) : Unit = {
        LOG.error(
            s"""${e.getClass}: ${e.getMessage}
               |${e.getCause}
               |${
                e.getStackTrace.mkString( "\n" )
            }""".stripMargin )
    }
}

object LocalReaderOutputStorageService {
    case class Dependencies( persistenceDirPath : String ) {
        def build() : LocalReaderOutputStorageService = new LocalReaderOutputStorageService( this )
    }

    def apply( persistenceDirPath : String ) : LocalReaderOutputStorageService =
        Dependencies( persistenceDirPath ).build()
}
