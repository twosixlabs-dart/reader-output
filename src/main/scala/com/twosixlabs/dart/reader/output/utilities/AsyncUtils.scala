package com.twosixlabs.dart.reader.output.utilities

import scala.concurrent.{ExecutionContextExecutor, Future, TimeoutException}
import scala.concurrent.duration.Duration

object AsyncUtils {

    def sleepAsync( duration : Duration )( implicit ec : ExecutionContextExecutor ) : Future[ Unit ] = Future {
        Thread.sleep( duration.toMillis )
    }

    implicit class FutureWithTimeout[ T ]( fut : Future[ T ] ) {
        def withTimeout( timeout : Duration )( implicit ec : ExecutionContextExecutor ) : Future[ T ] = {
            val sleepFuture = sleepAsync( timeout )
              .map( _ => throw new TimeoutException( s"Future timed out after $timeout" ) )
            Future.firstCompletedOf( List( sleepFuture, fut ) )
        }
    }


}
