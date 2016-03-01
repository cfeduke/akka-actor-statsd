package deploymentzone

import net.ceedubs.ficus._, Ficus._
import java.net.InetSocketAddress
import com.typesafe.config.{Config => TsConfig, _}
import scala.concurrent.duration._
import scala.util.matching.Regex


case class Config(
  address: InetSocketAddress,
  namespace: String,
  packetSize: Int,
  transmitInterval: FiniteDuration,
  enableMultiMetric: Boolean,
  transformations: Seq[Transform]
)

object Config {
  val path = "akka.statsd"

  def apply(underlying: TsConfig = ConfigFactory.load): Config = {
    val cfg = underlying.getConfig(path)

    def transformation(c: FicusConfig) =
      Transform(new Regex(c.as[String]("pattern")), c.as[String]("into"))

    Config(
      new InetSocketAddress(
        cfg.as[String]("hostname"),
        cfg.as[Int]("port")),
      namespace = cfg.as[String]("namespace"),
      packetSize = cfg.as[Int]("packet-size"),
      transmitInterval = cfg.as[FiniteDuration]("transmit-interval"),
      enableMultiMetric = cfg.as[Boolean]("enable-multi-metric"),
      transformations =
        cfg.as[Seq[FicusConfig]]("transform").map(transformation)
    )
  }
}
