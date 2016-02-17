package deploymentzone.actor

import net.ceedubs.ficus.Ficus._
import java.net.InetSocketAddress
import com.typesafe.config.{Config => TsConfig, _}
import scala.concurrent.duration._

case class Config private[actor](
  address: InetSocketAddress,
  namespace: String,
  packetSize: Int,
  transmitInterval: FiniteDuration,
  enableMultiMetric: Boolean
)

private[actor] object Config {
  val path = "deploymentzone.akka-actor-statsd"

  def apply(underlying: TsConfig = ConfigFactory.load): Config = {
    val cfg = underlying.getConfig(path)

    Config(
      new InetSocketAddress(
        cfg.as[String]("hostname"),
        cfg.as[Int]("port")),
      namespace = cfg.as[String]("namespace"),
      packetSize = cfg.as[Int]("packet-size"),
      transmitInterval = cfg.as[FiniteDuration]("transmit-interval"),
      enableMultiMetric = cfg.as[Boolean]("enable-multimetric")
    )
  }
}
