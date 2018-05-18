package akka.statsd

import java.net.InetSocketAddress
import com.typesafe.config.{Config => TsConfig, _}
import scala.concurrent.duration._
import scala.concurrent.duration.MILLISECONDS
import scala.collection.JavaConverters._

case class Config(
  address: InetSocketAddress,
  namespace: String,
  packetSize: Int,
  transmitInterval: FiniteDuration,
  enableMultiMetric: Boolean,
  emptyQueueOnFlush: Boolean,
  transformations: Seq[Transformation]
)

object Config {
  val path = "akka.statsd"

  def apply(underlying: TsConfig = ConfigFactory.load): Config = {
    val cfg = underlying.getConfig(path)

    def transformation(c: TsConfig) =
      Transformation(c.getString("pattern"), c.getString("into"))

    Config(
      new InetSocketAddress(cfg.getString("hostname"), cfg.getInt("port")),
      namespace = cfg.getString("namespace"),
      packetSize = cfg.getInt("packet-size"),
      transmitInterval =
        FiniteDuration(cfg.getDuration("transmit-interval", java.util.concurrent.TimeUnit.MILLISECONDS), MILLISECONDS),
      enableMultiMetric = cfg.getBoolean("enable-multi-metric"),
      emptyQueueOnFlush = cfg.getBoolean("empty-queue-on-flush"),
      transformations = cfg.getConfigList("transformations").asScala.map(transformation)
    )
  }
}
