package deploymentzone.actor

import net.ceedubs.ficus.FicusConfig._
import java.net.InetSocketAddress
import com.typesafe.config.ConfigFactory

private[actor] class Config(private val _hostname: Option[String], 
                            private val _port: Option[Int],
                            val underlyingConfig: com.typesafe.config.Config) {
  import Defaults._
  private val path = "deploymentzone.akka-actor-statsd"

  lazy val hostname = _hostname.getOrElse(underlyingConfig.getString(s"$path.hostname"))
  lazy val port = _port.getOrElse(underlyingConfig.as[Option[Int]](s"$path.port").getOrElse(STATSD_UDP_PORT))

  lazy val address = new InetSocketAddress(hostname, port)

  lazy val namespace = underlyingConfig.as[Option[String]](s"$path.namespace").getOrElse("")

}

private[actor] object Config {
  def apply(underlyingConfig: com.typesafe.config.Config) =
    new Config(None, None, underlyingConfig)
  def apply(hostname: String, port: Int) =
    new Config(Some(hostname), Some(port), Defaults.underlyingConfig)
}

private[actor] object Defaults {
  val STATSD_UDP_PORT = 8125
  
  private[actor] lazy val underlyingConfig = ConfigFactory.load()

  lazy val config: Config = Config(underlyingConfig)
}
