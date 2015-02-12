package deploymentzone.actor

import net.ceedubs.ficus.FicusConfig._
import java.net.InetSocketAddress
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

private[actor] class Config(private val _hostname: Option[String], 
                            private val _port: Option[Int],
                            val underlyingConfig: com.typesafe.config.Config) {
  import Defaults._
  import Config.path

  lazy val hostname = _hostname.getOrElse(underlyingConfig.getString(s"$path.hostname"))
  lazy val port = _port.getOrElse(underlyingConfig.as[Option[Int]](s"$path.port").getOrElse(STATSD_UDP_PORT))

  lazy val address = new InetSocketAddress(hostname, port)

  val namespace = underlyingConfig.as[Option[String]](s"$path.namespace").getOrElse("")

  val packetSize = underlyingConfig.as[Option[Int]](s"$path.packet-size").getOrElse(Defaults.PACKET_SIZE)

  val transmitInterval =
    underlyingConfig.as[Option[FiniteDuration]](s"$path.transmit-interval").getOrElse(Defaults.TRANSMIT_INTERVAL)

  val enableMultiMetric = underlyingConfig.as[Option[Boolean]](s"$path.enable-multimetric").getOrElse(Defaults.ENABLE_MULTIMETRIC)

}

private[actor] object Config {
  val path = "deploymentzone.akka-actor-statsd"

  def apply(underlyingConfig: com.typesafe.config.Config) =
    new Config(None, None, underlyingConfig)
  def apply(hostname: String, port: Int) =
    new Config(Some(hostname), Some(port), Defaults.underlyingConfig)
  def apply(address: InetSocketAddress) =
    new Config(Some(address.getHostName), Some(address.getPort), Defaults.underlyingConfig)
}

private[actor] object Defaults {
  val STATSD_UDP_PORT = 8125
  val PACKET_SIZE = PacketSize.FAST_ETHERNET
  val TRANSMIT_INTERVAL = 100.milliseconds
  val ENABLE_MULTIMETRIC = true
  
  private[actor] lazy val underlyingConfig = ConfigFactory.load()

  lazy val config: Config = Config(underlyingConfig)
}
