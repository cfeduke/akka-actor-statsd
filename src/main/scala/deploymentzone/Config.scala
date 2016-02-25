package deploymentzone

import net.ceedubs.ficus.Ficus._
import java.net.InetSocketAddress
import com.typesafe.config.{Config => TsConfig, _}
import scala.concurrent.duration._


case class Config(
  address: InetSocketAddress,
  namespace: String,
  packetSize: Int,
  transmitInterval: FiniteDuration,
  enableMultiMetric: Boolean
)

object Config {
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
      enableMultiMetric = cfg.as[Boolean]("enable-multi-metric")
    )
  }
}
