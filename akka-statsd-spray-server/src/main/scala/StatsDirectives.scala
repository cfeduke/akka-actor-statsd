package akka.statsd.spray.server

import com.typesafe.config.Config
import akka.actor.{ActorSystem, ActorRef}
import scala.util.matching.Regex
import spray._, http._, routing._, directives._, BasicDirectives._
import akka.statsd.{Config => StatsConfig, _}


trait StatsDirectives {

  /**
   * Override this in the calling class when you want to specify your own configuration
   */
  def statsConfig: StatsConfig = StatsConfig()

  /**
   * Implement this in the calling class.
   */
  def statsSystem: ActorSystem

  protected lazy val stats: ActorRef = statsSystem.actorOf(Stats.props(statsConfig))

  private def nowInMillis: Long = System.currentTimeMillis

  private lazy val server = Bucket("http.server", statsConfig.transformations)

  private def success(m: HttpMethod, p: Uri.Path): Bucket =
    server / m.name.toLowerCase / p.toString

  private def failure(m: HttpMethod, p: Uri.Path, c: StatusCode): Bucket =
    server / c.intValue.toString / p.toString


  /**
   * Collects timing and number of requests and sends data to statsd server
   */
  def countAndTime: Directive0 =
    mapRequestContext { ctx =>
      val start = nowInMillis
      val req = ctx.request
      ctx.withHttpResponseMapped { response =>
        if (response.status.isSuccess) {
          val b = success(req.method, req.uri.path)
          val end = nowInMillis
          stats ! Increment(b)
          stats ! new Timing(b)(end - start)
        } else
          stats ! Increment(failure(req.method, req.uri.path, response.status))

        response
      }
    }

  /**
   * Collects timing of requests and sends data to statsd server
   */
  def time: Directive0 =
    mapRequestContext { ctx =>
      val start = nowInMillis
      val req = ctx.request

      ctx.withHttpResponseMapped { r =>
        if (r.status.isSuccess)
          stats ! new Timing(success(req.method, req.uri.path))(nowInMillis - start)
        r
      }
    }

  /**
   * Collects number of requests and sends data to statsd
   */
  def count: Directive0 =
    mapRequest { req =>
      stats ! Increment(success(req.method, req.uri.path))
      req
    }

}
