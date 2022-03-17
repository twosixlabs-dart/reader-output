package com.twosixlabs.dart.reader.output.models.tables

import com.twosixlabs.dart.reader.output.models.ReaderModels.ReaderOutputMetadataRecord
import slick.lifted.ProvenShape
import slick.sql.SqlProfile.ColumnOption.SqlType

import java.time.LocalDateTime


object Schema {

    import com.twosixlabs.dart.reader.output.configuration.PgSlickProfile.api._

    trait Selector {
        def sortFields : Map[ String, Rep[ _ ] ]
    }

    class ReaderOutputTable( tag : Tag ) extends Table[ ReaderOutputMetadataRecord ]( tag : Tag,"reader_output" ) with Selector {

        def readerId : Rep[ String ] = column[ String ]( "reader_id" )

        def readerVersion : Rep[ String ] = column[ String ]( "reader_version" )

        def documentId : Rep[ String ] = column[ String ]( "document_id" )

        def storageKey : Rep[ String ] = column[ String ]( "storage_key" )

        def outputVersion : Rep[ Option[ String ] ] = column[ Option[ String ] ]( "output_version" )

        def labels : Rep[ List[ String ] ] = column[ List[ String ] ]( "labels", O.Default( Nil ) )

        def timestamp : Rep[ LocalDateTime ] = column[ LocalDateTime ]( "timestamp", SqlType( "timestamp not null default CURRENT_TIMESTAMP" ) )

        override def * : ProvenShape[ ReaderOutputMetadataRecord ] =
            (readerId, readerVersion, documentId, storageKey, outputVersion, labels, timestamp) <>
            (ReaderOutputMetadataRecord.tupled, ReaderOutputMetadataRecord.unapply)

        val sortFields : Map[ String, Rep[ _ ] ] =
            Map(
                "readerId" -> this.readerId,
                "readerVersion" -> this.readerVersion,
                "outputVersion" -> this.outputVersion,
            )
    }

    val readerOutputTableQuery = TableQuery[ ReaderOutputTable ]

    val readerOutputTableInsert = readerOutputTableQuery
      .map( t => (t.readerId, t.readerVersion, t.documentId, t.storageKey, t.outputVersion, t.labels) )

    val readerOutputTableInsertWithoutLabels = readerOutputTableQuery
      .map( t => (t.readerId, t.readerVersion, t.documentId, t.outputVersion, t.storageKey) )
}

