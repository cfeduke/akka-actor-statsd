package akka.statsd.http.server

import akka.{Done, NotUsed}
import akka.http.scaladsl.server.Directives.{extractRequestContext, mapRouteResult}
import akka.http.scaladsl.server.{Directive0, RequestContext, RouteResult}
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import scala.concurrent.Future
import scala.util.{Success, Try}

trait AroundDirectives {

  /**
    * @param onRequest Called when a request starts; the result (a function
    *                  `Try[RouteResult => Unit]`) is applied to when the
    *                  request completes either successfully or fails.
    */
  def aroundRequest[T](onRequest: RequestContext => Try[RouteResult] => Unit): Directive0 =
    extractRequestContext.flatMap { ctx =>
      val onDone = onRequest(ctx)
      import ctx.executionContext
      mapRouteResult {
        case c @ Complete(response) =>
          Complete(response.mapEntity { entity =>
            if (entity.isKnownEmpty()) {
              // On an empty entity, `transformDataBytes` unsets `isKnownEmpty`.
              // Call onDone right away, since there's no significant amount of
              // data to send, anyway.
              onDone(Success(c))
              entity
            } else {
              entity.transformDataBytes(Flow[ByteString].watchTermination() {
                case (m: NotUsed, f: Future[Done]) =>
                  f.map(_ => c).onComplete(onDone)
                  m
              })
            }
          })
        case r: Rejected =>
          onDone(Success(r))
          r
      }
    }

}
