package deploymentzone.spray

import akka.actor.{ActorSystem, ActorRef}
import com.typesafe.config.Config
import deploymentzone.{Config => StatsConfig, _}, actor._
import scala.util.matching.Regex
import _root_.spray._, http._, routing._, directives._
import BasicDirectives._


trait StatsDirectives {
  import StatsDirectives._

  /**
   * Override this in the calling class when you want to specify your own configuration
   */
  def statsConfig: StatsConfig = StatsConfig()

  /**
   * Implement this in the calling class.
   */
  def statsSystem: ActorSystem

  private def nowInMillis: Long = System.currentTimeMillis

  private def successBucket(r: HttpRequest) =
    bucket(r, statsConfig.transformations)

  /**
   * Collects timing and number of requests and sends data to statsd server
   */
  def countAndTime: Directive0 =
    mapRequestContext { ctx =>
      val start = nowInMillis
      val req = ctx.request
      ctx.withHttpResponseMapped { response =>
        if (response.status.isSuccess) {
          val b = successBucket(req)
          val end = nowInMillis
          stats ! Increment(b)
          stats ! new Timing(b)(end - start)
        } else
          stats ! Increment(failureBucket(response.status.intValue))

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
        if (r.status.isSuccess) stats ! new Timing(successBucket(req))(nowInMillis - start)
        r
      }
    }

  /**
   * Collects number of requests and sends data to statsd
   */
  def count: Directive0 =
    mapRequestContext { ctx =>
      stats ! Increment(successBucket(ctx.request))
      ctx
    }

  private[spray] lazy val statsActors = {
    import collection.JavaConverters._
    (new java.util.concurrent.ConcurrentHashMap[String, ActorRef]).asScala
  }

  /**
   * Supports multiple actor systems: singleton pattern to create one statsActor per actor system.
   * @return a statsActor for the specified system and/or configuration
   */
  private def stats: ActorRef =
    statsActors.getOrElseUpdate(
      statsSystem.name,
      statsSystem.actorOf(Stats.props(statsConfig)))

}

private[spray] object StatsDirectives {
  val UUID_Pattern = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}".r

  def bucket(r: HttpRequest, transformations: Seq[Transform]): String = {
    val path = {
      val rendered = r.uri.path.toString
      val transformed = transformations
        .find(_.pattern.findFirstIn(rendered).isDefined)
        .map(t => t.pattern.replaceAllIn(rendered, t.into))
        .getOrElse(rendered)

      UUID_Pattern
        .replaceAllIn(transformed, "[id]")
        .replaceAll("/", ".")
        .replaceAll("^\\.$", ".ROOT")
    }

    r.method.name.toLowerCase + path
  }

  def failureBucket(s: StatusCode): String =
    s.intValue + "xx." + s.value

}
