package com.twosixlabs.dart.reader.output.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonInclude, JsonProperty}
import com.twosixlabs.dart.reader.output.models.QueryModels.ReaderOutputMetadataQueryResult

import java.time.LocalDateTime
import scala.beans.BeanProperty

object ReaderModels {

    // Represents a row in the readers output table
    case class ReaderOutputMetadataRecord( identity : String,
                                           version : String,
                                           documentId : String,
                                           storageKey : String,
                                           outputVersion : Option[ String ],
                                           labels : List[ String ],
                                           timestamp : LocalDateTime ) {
        def toQueryResult( tenants : Iterable[ String ] = Nil ) : ReaderOutputMetadataQueryResult =
            ReaderOutputMetadataQueryResult( identity, version, documentId, storageKey, outputVersion, tenants.toList, labels, timestamp )

        def toSubmissionResult : ReaderOutputSubmissionResult =
            ReaderOutputSubmissionResult( identity, version, documentId, storageKey, outputVersion, Some( labels.toSet ) )
    }

    // Metadata provided with reader output upload
    @JsonInclude( Include.NON_ABSENT )
    case class ReaderOutputMetadataSubmission( @BeanProperty @JsonProperty( value = "identity", required = true ) identity : String,
                                               @BeanProperty @JsonProperty( value = "version", required = true ) version : String,
                                               @BeanProperty @JsonProperty( value = "document_id", required = true ) documentId : String,
                                               @BeanProperty @JsonProperty( value = "output_version", required = false ) outputVersion : Option[ String ],
                                               @BeanProperty @JsonProperty( value = "labels", required = false ) labels : Option[ Set[ String ] ] ) {
        def toTuple( storageKey : String ) : (String, String, String, String, Option[ String ], List[ String ]) = (identity, version, documentId, storageKey, outputVersion,
          labels.map( _.toList ).getOrElse( Nil ))
    }

    // Response to reader output upload
    @JsonInclude( Include.NON_ABSENT )
    case class ReaderOutputSubmissionResult( @BeanProperty @JsonProperty( value = "identity", required = true ) identity : String,
                                             @BeanProperty @JsonProperty( value = "version", required = true ) version : String,
                                             @BeanProperty @JsonProperty( value = "document_id", required = true ) documentId : String,
                                             @BeanProperty @JsonProperty( value = "storage_key", required = true ) storageKey : String,
                                             @BeanProperty @JsonProperty( value = "output_version", required = false ) outputVersion : Option[ String ],
                                             @BeanProperty @JsonProperty( value = "labels", required = false ) labels : Option[ Set[ String ] ] )

}
