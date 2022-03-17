package com.twosixlabs.dart.test.utilities

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}
import scala.util.Try

object TestUtils {

    lazy val inGitlabCi : Boolean =
        Try( Option( System.getenv( "GITLAB_CI" ) ) )
          .toOption
          .flatten
          .map( _.trim.toLowerCase )
          .contains( "true" )

    def getConfig : Config = {
        if ( inGitlabCi ) ConfigFactory.parseResources( "test-gitlab.conf" ).resolve()
        else ConfigFactory.parseResources( "test-local.conf" ).resolve()

    }

    class FutureImplicits( defaultTimeout : Long, writeDelay : Long ) {

        implicit class AwaitableFuture[ T ]( fut : Future[ T ] ) {
            def await( ms : Long = defaultTimeout ) : T = Await.result( fut, ms.milliseconds )

            def await : T = await()

            def awaitTry( ms : Long = defaultTimeout ) : Try[ T ] = Await.ready( fut, ms.milliseconds ).value.get

            def awaitTry : Try[ T ] = awaitTry()

            def awaitWrite( to : Long = defaultTimeout, wd : Long = writeDelay ) : T = {
                val res = await( to )
                Thread.sleep( wd )
                res
            }

            def awaitWrite : T = awaitWrite()

            def awaitWriteTry( to : Long = defaultTimeout, wd : Long = writeDelay ) : Try[ T ] = {
                val res = awaitTry( to )
                Thread.sleep( wd )
                res
            }

            def awaitWriteTry : Try[ T ] = awaitWriteTry()
        }
    }

}
