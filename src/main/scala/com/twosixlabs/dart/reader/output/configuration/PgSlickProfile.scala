package com.twosixlabs.dart.reader.output.configuration

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities


trait PgSlickProfile extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgRangeSupport
  with PgHStoreSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {
    def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

    // Add back `capabilities.insertOrUpdate` to enable native `upsert` support; for postgres 9.5+
    override protected def computeCapabilities : Set[ Capability ] =
        super.computeCapabilities + JdbcCapabilities.insertOrUpdate

    override val api = PgSlickApi

    object PgSlickApi extends API with ArrayImplicits
      with DateTimeImplicits
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants {
        implicit val strListTypeMapper : DriverJdbcType[ List[ String ] ] = new SimpleArrayJdbcType[ String ]( "text" ).to( _.toList )
    }

}

object PgSlickProfile extends PgSlickProfile
