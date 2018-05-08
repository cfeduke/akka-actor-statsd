package akka.statsd.http.server

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpMethod, StatusCode, Uri}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.RouteResult.{Complete, Rejected}
import akka.statsd.{Config => StatsConfig, _}
import akka.http.scaladsl.util.FastFuture._
import scala.util.{Failure, Success, Try}

trait StatsDirectives extends AroundDirectives {

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

  private def bucket(bucketName: String) = Bucket(bucketName , statsConfig.transformations)
  private val defaultBucket = "http.server"

  private def success(m: HttpMethod, p: Uri.Path, initBucket: String): Bucket =
    bucket(initBucket) / m.name.toLowerCase / p.toString

  private def failure(m: HttpMethod, p: Uri.Path, c: StatusCode, initBucket: String): Bucket =
    bucket(initBucket) / c.intValue.toString / p.toString

  private def exception(m: HttpMethod, p: Uri.Path, initBucket: String): Bucket =
    bucket(initBucket) / "exception" / p.toString


  /**
    * Collects timing and number of requests and sends data to statsd server
    * @param baseBucket is the initial bucket in which all metrics will be generated
    */
  def countAndTimeInBucket(baseBucket: String): Directive0 =
    countInBucket(baseBucket) & timeInBucket(baseBucket)
  /**
    * Collects timing and number of requests and sends data to statsd server
    * with defaultBucket
    */
  def countAndTime: Directive0 = countAndTimeInBucket(defaultBucket)

  private def timeRequest(baseBucket: String)(ctx: RequestContext): Try[RouteResult] => Unit = {
    val start = nowInMillis

    {
      case Success(Complete(response)) =>
        if (response.status.isSuccess) {
          stats ! new Timing(success(ctx.request.method, ctx.request.uri.path, baseBucket))(nowInMillis - start)
        }
      case Success(Rejected(_)) =>
      case Failure(_)           =>
    }
  }

  /**
    * Collects timing of requests and sends data to statsd server
    * @param baseBucket is the initial bucket in which all metrics will be generated
    */
  def timeInBucket(baseBucket: String) : Directive0 =
    aroundRequest(timeRequest(baseBucket))

  /**
    * Collects timing and number of requests and sends data to statsd server
    * with defaultBucket
    */
  def time: Directive0 = timeInBucket(defaultBucket)

  def countInBucket(baseBucket: String): Directive0 = {
    Directive { innerRouteBuilder ⇒ ctx ⇒
      import ctx.executionContext

      val req = ctx.request

      val x = innerRouteBuilder(())(ctx).fast.map {
        case res@Complete(response) =>
          if (response.status.isSuccess) {
            stats ! Increment(success(req.method, req.uri.path, baseBucket))
          } else {
            stats ! Increment(failure(req.method, req.uri.path, response.status, baseBucket))
          }
          res
        case res =>
          res
      }

      x.failed.foreach {
        _ => stats ! Increment(exception(req.method, req.uri.path, baseBucket))
      }
      x
    }
  }



  /**
   * Collects number of requests and sends data to statsd
   * with defaultBucket
   */
  def count: Directive0 = countInBucket(defaultBucket)

}
