package akka.statsd.spray.client

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Success, Failure}

import akka.actor.ActorRef
import spray.http._
import spray.httpx.{RequestBuilding, ResponseTransformation}
import spray.client.pipelining.{sendReceive => spraySendReceive, _}
import akka.statsd.{Config => StatsConfig, _}


trait MeasuringPipeline
  extends RequestBuilding
  with ResponseTransformation {

  def statistician: ActorRef
  def statsConfig: StatsConfig = StatsConfig()

  def measuring(pipeline: SendReceive)(implicit ec: ExecutionContext): SendReceive =
    req => {
      val start = System.currentTimeMillis()

      pipeline(req) andThen {
        case Success(res) =>
          val bucket =
            if (res.status.isSuccess) success(req.method, req.uri)
            else failure(req.method, req.uri)(res.status)
          val end = System.currentTimeMillis
          statistician ! Timing(bucket)((end - start).milliseconds)
          statistician ! Increment(bucket)
        case Failure(_) =>
          statistician ! Increment(error(req.method, req.uri))
      }
    }

  private lazy val client = Bucket("http.client", statsConfig.transformations)

  private def bucket(outcome: String)(m: HttpMethod, uri: Uri): Bucket = {
    val withMethod = client / outcome / m.name.toLowerCase
    val withHost = withMethod / uri.authority.host.toString
    val withPort =
      if (uri.authority.port != 0) withHost / uri.authority.port.toString
      else withHost

    withPort / uri.path.toString
  }

  private lazy val success = bucket("success") _
  private lazy val error = bucket("error") _
  private def failure(m: HttpMethod, uri: Uri)(c: StatusCode) = bucket(s"failure/${c.intValue}")(m, uri)
}

