package akka.statsd.spray.server

import akka.actor.{ActorSystem, ActorRef}
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

  private def bucket(bucketName: String) = Bucket(bucketName , statsConfig.transformations)
  private val defaultBucket = "http.server"

  private def success(m: HttpMethod, p: Uri.Path, initBucket: String): Bucket =
    bucket(initBucket) / m.name.toLowerCase / p.toString

  private def failure(m: HttpMethod, p: Uri.Path, c: StatusCode, initBucket: String): Bucket =
    bucket(initBucket) / c.intValue.toString / p.toString


  /**
    * Collects timing and number of requests and sends data to statsd server
    * @param baseBucket is the initial bucket in which all metrics will be generated
    */
  def countAndTimeInBucket(baseBucket: String): Directive0 =
    mapRequestContext { ctx =>
      val start = nowInMillis
      val req = ctx.request
      ctx.withHttpResponseMapped { response =>
        if (response.status.isSuccess) {
          val b = success(req.method, req.uri.path, baseBucket)
          val end = nowInMillis
          stats ! Increment(b)
          stats ! new Timing(b)(end - start)
        } else
          stats ! Increment(failure(req.method, req.uri.path, response.status, baseBucket))

        response
      }
    }
  /**
    * Collects timing and number of requests and sends data to statsd server
    * with defaultBucket
    */
  def countAndTime: Directive0 = countAndTimeInBucket(defaultBucket)

  /**
    * Collects timing of requests and sends data to statsd server
    * @param baseBucket is the initial bucket in which all metrics will be generated
    */
  def timeInBucket(baseBucket: String) : Directive0 =
    mapRequestContext { ctx =>
      val start = nowInMillis
      val req = ctx.request

      ctx.withHttpResponseMapped { r =>
        if (r.status.isSuccess)
          stats ! new Timing(success(req.method, req.uri.path, baseBucket))(nowInMillis - start)
        r
      }
    }

  /**
    * Collects timing and number of requests and sends data to statsd server
    * with defaultBucket
    */
  def time: Directive0 = timeInBucket(defaultBucket)

  /**
   * Collects number of requests and sends data to statsd
   * @param baseBucket is the initial bucket in which all metrics will be generated
   */
  def countInBucket(baseBucket: String): Directive0 =
    mapRequest { req =>
      stats ! Increment(success(req.method, req.uri.path, baseBucket))
      req
    }
  /**
   * Collects number of requests and sends data to statsd
   * with defaultBucket
   */
  def count: Directive0 = countInBucket(defaultBucket)

}
