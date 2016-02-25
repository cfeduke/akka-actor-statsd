package deploymentzone

import org.scalatest.{FunSpec, Matchers}
import com.typesafe.config.{ConfigFactory, ConfigException}


class ConfigSpec
  extends FunSpec
  with Matchers {

  describe("Config") {
    it("falls back to the default port when its not specified") {
      val config = Config(ConfigFactory.load("ConfigSpecWithoutPort.conf"))
      config.address.getPort should be(8125)
    }

    it("incomplete unless hostname is specified") {
      intercept[ConfigException.Missing]  {
        Config(ConfigFactory.empty())
      }
    }

    it("has overridable defaults") {
      val config = Config(ConfigFactory.load("ConfigSpec.conf"))
      config.address.getAddress.getHostAddress should be("127.0.0.1")
      config.address.getPort should be(9999)
      config.namespace should be("mango")
    }
  }
}
