package akka.statsd.http.client

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.statsd.{Config => StatsConfig, _}
import akka.http.scaladsl.model._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.Future
import StatsClient.ReqRes

trait StatsClient {

  protected def bucket(bucketName: String)(implicit statsConfig: StatsConfig) =
    Bucket(bucketName, statsConfig.transformations)

  protected def uriToString(u: Uri): String = u.scheme + '.' + u.authority.toString.replace('.', '-') + u.path.toString

  protected def responseBucket(baseBucket: String, m: HttpMethod, u: Uri, c: StatusCode)(
    implicit statsConfig: StatsConfig
  ): Bucket = {
    val statusBucket = c match {
      case x if x.intValue >= 200 && x.intValue <= 299 => "2xx"
      case x if x.intValue >= 300 && x.intValue <= 399 => "3xx"
      case x if x.intValue >= 400 && x.intValue <= 499 => "4xx"
      case x if x.intValue >= 500 && x.intValue <= 599 => "5xx"
      case _                                           => "unknown"
    }

    bucket(baseBucket) / "response" / m.name.toLowerCase / statusBucket / uriToString(u)
  }

  protected def exceptionBucket(baseBucket: String, m: HttpMethod, u: Uri)(implicit statsConfig: StatsConfig): Bucket =
    bucket(baseBucket) / "response" / m.name.toLowerCase / "exception" / uriToString(u)

  protected def requestBucket(baseBucket: String, m: HttpMethod, u: Uri)(implicit statsConfig: StatsConfig): Bucket =
    bucket(baseBucket) / "request" / m.name.toLowerCase / uriToString(u)

  protected def nowInNanos: Long = System.nanoTime

  protected val defaultBucket = "http.client"

  protected def statsActor(system: ActorSystem, statsConfig: StatsConfig): ActorRef =
    StatsExtension(system).statsActor(statsConfig)

  def countRequests(
    underlying: ReqRes,
    baseBucket: String = defaultBucket
  )(implicit system: ActorSystem, statsConfig: StatsConfig = StatsConfig()): ReqRes = { req =>
    val stats = statsActor(system, statsConfig)
    stats ! Increment(requestBucket(baseBucket, req.method, req.uri))
    underlying(req)
  }

  def timeResponses(
    underlying: ReqRes,
    baseBucket: String = defaultBucket
  )(implicit system: ActorSystem, statsConfig: StatsConfig = StatsConfig()): ReqRes = { req =>
    import system.dispatcher

    val stats = statsActor(system, statsConfig)

    val start = nowInNanos

    underlying(req).map { response =>
      val bucket = responseBucket(baseBucket, req.method, req.uri, response.status)
      stats ! new Timing(bucket)(Duration(nowInNanos - start, TimeUnit.NANOSECONDS).toMillis)
      response
    }
  }

  def countResponses(
    underlying: ReqRes,
    baseBucket: String = defaultBucket
  )(implicit system: ActorSystem, statsConfig: StatsConfig = StatsConfig()): ReqRes = { req =>
    import system.dispatcher
    val stats = statsActor(system, statsConfig)

    val res = underlying(req).map { response =>
      val bucket = responseBucket(baseBucket, req.method, req.uri, response.status)
      stats ! Increment(bucket)
      response
    }

    res.onComplete {
      case scala.util.Failure(_) =>
        stats ! Increment(exceptionBucket(baseBucket, req.method, req.uri))
      case scala.util.Success(_) =>
    }

    res
  }
}

object StatsClient extends StatsClient {
  type ReqRes = HttpRequest => Future[HttpResponse]

  def apply(baseBucket: String = defaultBucket)(
    implicit system: ActorSystem,
    statsConfig: StatsConfig = StatsConfig()
  ): ReqRes = {
    val underlying: ReqRes = req => Http(system).singleRequest(req)

    Function.chain(
      Seq(
        countRequests(_: ReqRes, baseBucket),
        countResponses(_: ReqRes, baseBucket),
        timeResponses(_: ReqRes, baseBucket)
      )
    )(underlying)
  }
}
