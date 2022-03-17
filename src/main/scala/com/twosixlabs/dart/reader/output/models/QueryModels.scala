package com.twosixlabs.dart.reader.output.models

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonInclude, JsonProperty}
import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer

import java.time.LocalDateTime
import scala.beans.BeanProperty

object QueryModels {

    // Sub-query within query object to filter based on timestamp
    @JsonInclude( Include.NON_ABSENT )
    case class TimestampQuery( @BeanProperty @JsonProperty( value = "before", required = false ) @JsonSerialize( using = classOf[ LocalDateTimeSerializer ] ) @JsonDeserialize( using = classOf[ LocalDateTimeDeserializer ] ) before : LocalDateTime = null,
                               @BeanProperty @JsonProperty( value = "after", required = false ) @JsonSerialize( using = classOf[ LocalDateTimeSerializer ] ) @JsonDeserialize( using = classOf[ LocalDateTimeDeserializer ] ) after : LocalDateTime = null,
                               @BeanProperty @JsonProperty( value = "on", required = false ) @JsonSerialize( using = classOf[ LocalDateTimeSerializer ] ) @JsonDeserialize( using = classOf[ LocalDateTimeDeserializer ] ) on : LocalDateTime = null ) {

        @JsonIgnore
        def isEmpty : Boolean = {
            before == null && after == null && on == null
        }

    }

    // Full query object
    @JsonInclude( Include.NON_ABSENT )
    case class ReaderOutputQuery( @BeanProperty @JsonProperty( value = "readers", required = false ) readers : Option[ List[ String ] ] = None,
                                  @BeanProperty @JsonProperty( value = "versions", required = false ) versions : Option[ List[ String ] ] = None,
                                  @BeanProperty @JsonProperty( value = "document_ids", required = false ) documentIds : Option[ List[ String ] ] = None,
                                  @BeanProperty @JsonProperty( value = "output_versions", required = false ) outputVersions : Option[ List[ String ] ] = None,
                                  @BeanProperty @JsonProperty( value = "labels", required = false ) labels : Option[ List[ String ] ] = None,
                                  @BeanProperty @JsonProperty( value = "tenant_id", required = false ) tenantId : Option[ String ] = None,
                                  @BeanProperty @JsonProperty( value = "timestamp", required = false ) timestamp : Option[ TimestampQuery ] = None ) {

        @JsonIgnore
        def isEmpty : Boolean = {
            readers.isEmpty && versions.isEmpty && documentIds.isEmpty && timestamp.isEmpty && outputVersions.isEmpty && labels.isEmpty && tenantId.isEmpty
        }
    }

    // Single search result -- note timestamp is included for testing purposes
    // but not serialized
    @JsonInclude( Include.NON_ABSENT )
    @JsonIgnoreProperties( Array( "timestamp" ) )
    case class ReaderOutputMetadataQueryResult( @BeanProperty @JsonProperty( "identity" ) identity : String,
                                                @BeanProperty @JsonProperty( "version" ) version : String,
                                                @BeanProperty @JsonProperty( "document_id" ) documentId : String,
                                                @BeanProperty @JsonProperty( "storage_key" ) storageKey : String,
                                                @BeanProperty @JsonProperty( "output_version" ) outputVersion : Option[ String ],
                                                @BeanProperty @JsonProperty( "tenants" ) tenants : List[ String ],
                                                @BeanProperty @JsonProperty( "labels" ) labels : List[ String ],
                                                @BeanProperty @JsonProperty( "timestamp" ) @JsonSerialize( using = classOf[ LocalDateTimeSerializer ] ) @JsonDeserialize( using = classOf[ LocalDateTimeDeserializer ] ) timestamp : LocalDateTime )

    // Wrapper of all results from a search
    case class ReaderOutputQueryResults(
      @BeanProperty @JsonProperty( value = "records", required = true ) records : Seq[ ReaderOutputMetadataQueryResult ] ) {
    }

}
