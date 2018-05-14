package akka.statsd.http.client

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.HttpExt
import akka.statsd._
import akka.http.scaladsl.model._
import akka.statsd.{Config => StatsConfig}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class StatsClient(
  requestMaker: HttpRequest => Future[HttpResponse],
  baseBucket: String = "http.client"
)(implicit system: ActorSystem) {

  /**
    * Override this in the calling class when you want to specify your own configuration
    */
  def statsConfig: StatsConfig = StatsConfig()

  protected def nowInNanos: Long = System.nanoTime

  protected def bucket(bucketName: String) = Bucket(bucketName, statsConfig.transformations)

  protected def uriToString(u: Uri): String = u.scheme + '.' + u.authority.toString.replace('.', '-') + u.path.toString

  protected def requestBucket(m: HttpMethod, u: Uri): Bucket =
    bucket(baseBucket) / "request" / m.name.toLowerCase / uriToString(u)

  protected def responseBucket(m: HttpMethod, u: Uri, c: StatusCode): Bucket = {
    val statusBucket = c match {
      case x if x.intValue >= 200 && x.intValue <= 299 => "2xx"
      case x if x.intValue >= 300 && x.intValue <= 399 => "3xx"
      case x if x.intValue >= 400 && x.intValue <= 499 => "4xx"
      case x if x.intValue >= 500 && x.intValue <= 599 => "5xx"
      case _                                           => "unknown"
    }

    bucket(baseBucket) / "response" / m.name.toLowerCase / statusBucket / uriToString(u)
  }

  protected def exceptionBucket(m: HttpMethod, u: Uri): Bucket =
    bucket(baseBucket) / "response" / m.name.toLowerCase / "exception" / uriToString(u)

  protected def createStatsActor(): ActorRef = system.actorOf(Stats.props(statsConfig))

  protected lazy val stats: ActorRef = createStatsActor()

  protected def sendRequestCount(bucket: Bucket): Unit = ()

  protected def sendResponseCount(bucket: Bucket): Unit = ()

  protected def sendResponseTime(bucket: Bucket, time: Long): Unit = ()

  def singleRequest(req: HttpRequest)(
    implicit ec: ExecutionContext
  ): Future[HttpResponse] = {
    val start = nowInNanos

    sendRequestCount(requestBucket(req.method, req.uri))

    val res = requestMaker(req).map { response =>
      val bucket = responseBucket(req.method, req.uri, response.status)
      sendResponseCount(bucket)
      sendResponseTime(bucket, Duration(nowInNanos - start, TimeUnit.NANOSECONDS).toMillis)
      response
    }

    res.onComplete {
      case scala.util.Failure(_) =>
        exceptionBucket(req.method, req.uri)
      case scala.util.Success(_) =>
    }

    res
  }
}

trait TimeResponses {
  self: StatsClient =>
  override protected def sendResponseTime(bucket: Bucket, time: Long): Unit = stats ! new Timing(bucket)(time)
}

trait CountRequests {
  self: StatsClient =>
  override protected def sendRequestCount(bucket: Bucket): Unit = stats ! Increment(bucket)
}

trait CountResponses {
  self: StatsClient =>
  override protected def sendResponseCount(bucket: Bucket): Unit = stats ! Increment(bucket)

}

object StatsClient {
  def apply(http: HttpExt, baseBucket: String = "http.client"): StatsClient =
    new StatsClient(req => http.singleRequest(req), baseBucket)(http.system)
      with CountRequests
      with CountResponses
      with TimeResponses
}
