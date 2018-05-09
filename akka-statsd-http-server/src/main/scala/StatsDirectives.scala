package akka.statsd.http.server

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpMethod, StatusCode, Uri}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.http.scaladsl.server.directives.BasicDirectives
import akka.statsd.{Config => StatsConfig, _}
import akka.http.scaladsl.util.FastFuture._
import scala.util.{Failure, Success, Try}

trait StatsDirectives extends AroundDirectives with BasicDirectives {

  /**
    * Override this in the calling class when you want to specify your own configuration
    */
  def statsConfig: StatsConfig = StatsConfig()

  protected lazy val extractStats: Directive1[ActorRef] =
    extractActorSystem.map {
      _.actorOf(Stats.props(statsConfig))
    }

  private def nowInMillis: Long = System.currentTimeMillis

  private def bucket(bucketName: String) = Bucket(bucketName, statsConfig.transformations)
  private val defaultBucket = "http.server"

  private def requestBucket(m: HttpMethod, p: Uri.Path, initBucket: String): Bucket =
    bucket(initBucket) / "request" / m.name.toLowerCase / p.toString

  private def responseBucket(m: HttpMethod, p: Uri.Path, c: StatusCode, initBucket: String): Bucket = {
    val statusBucket = c match {
      case x if x.intValue >= 200 && x.intValue <= 299 => "2xx"
      case x if x.intValue >= 300 && x.intValue <= 399 => "3xx"
      case x if x.intValue >= 400 && x.intValue <= 499 => "4xx"
      case x if x.intValue >= 500 && x.intValue <= 599 => "5xx"
      case _                                           => "unknown"
    }

    bucket(initBucket) / "response" / m.name.toLowerCase / statusBucket / p.toString
  }

  private def exceptionBucket(m: HttpMethod, p: Uri.Path, initBucket: String): Bucket =
    bucket(initBucket) / "response" / m.name.toLowerCase / "exception" / p.toString

  private def timeRequest(stats: ActorRef, baseBucket: String)(ctx: RequestContext): Try[RouteResult] => Unit = {
    val start = nowInMillis

    {
      case Success(Complete(response)) =>
        if (response.status.isSuccess) {
          stats ! new Timing(responseBucket(ctx.request.method, ctx.request.uri.path, response.status, baseBucket))(
            nowInMillis - start
          )
        }
      case Success(Rejected(_)) =>
      case Failure(_)           =>
    }
  }

  /**
    * Collects timing of requests and sends data to statsd server
    * @param baseBucket is the initial bucket in which all metrics will be generated
    */
  def timeInBucket(baseBucket: String): Directive0 =
    extractStats.flatMap { stats =>
      aroundRequest(timeRequest(stats, baseBucket))
    }

  /**
    * Collects timing and number of requests and sends data to statsd server with defaultBucket
    */
  def time: Directive0 = timeInBucket(defaultBucket)

  def countRequestInBucket(baseBucket: String): Directive0 = {
    extractStats.flatMap { stats =>
      Directive { innerRouteBuilder ⇒ ctx ⇒
        val req = ctx.request
        stats ! Increment(requestBucket(req.method, req.uri.path, baseBucket))
        innerRouteBuilder(())(ctx)
      }
    }
  }

  /**
    * Collects number of request and sends data to statsd with defaultBucket
    */
  def countRequest: Directive0 = countRequestInBucket(defaultBucket)

  def countResponseInBucket(baseBucket: String): Directive0 = {
    extractStats.flatMap { stats =>
      Directive { innerRouteBuilder ⇒ ctx ⇒
        import ctx.executionContext

        val req = ctx.request

        val x = innerRouteBuilder(())(ctx).fast.map {
          case res @ Complete(response) =>
            stats ! Increment(responseBucket(req.method, req.uri.path, response.status, baseBucket))
            res
          case res =>
            res
        }

        x.failed.foreach {
          _ => stats ! Increment(exceptionBucket(req.method, req.uri.path, baseBucket))
        }
        x
      }
    }
  }

  /**
    * Collects number of responses and sends data to statsd with defaultBucket
    */
  def countResponse: Directive0 = countResponseInBucket(defaultBucket)

  /**
    * Collects timing and number of requests & responses, sending data to statsd server
    * @param baseBucket is the initial bucket in which all metrics will be generated
    */
  def countAndTimeInBucket(baseBucket: String): Directive0 =
    countRequestInBucket(baseBucket) & countResponseInBucket(baseBucket) & timeInBucket(baseBucket)

  /**
    * Collects timing and number of requests and sends data to statsd server
    * with defaultBucket
    */
  def countAndTime: Directive0 = countAndTimeInBucket(defaultBucket)

}
