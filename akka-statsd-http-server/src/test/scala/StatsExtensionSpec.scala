package akka.statsd.http.server

import akka.actor.ActorSystem
import akka.statsd._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.{path => _, _}

class StatsExtensionSpec
  extends {
    override implicit val system = ActorSystem()
  }
  with TestKitBase
  with FunSpecLike
  with MustMatchers {

  val statsConfig =
    Config(ConfigFactory.parseString(
      """
        {
          akka.statsd.hostname = localhost
          akka.statsd.transformations = [
            {
              pattern = "/foo/[a-zA-Z0-9\\-]+/bar"
              into = "foo.[segment].bar"
            }
          ]
        }
      """
    ).withFallback(ConfigFactory.load))

  describe("stats extension") {
    it("gets the same actor for the same config") {
      val actor1 = StatsExtension(system).statsActor(statsConfig)
      val actor2 = StatsExtension(system).statsActor(statsConfig)

      actor1 must equal(actor2)
    }

    it("gets a different actor for a different config") {
      val actor1 = StatsExtension(system).statsActor(statsConfig)
      val actor2 = StatsExtension(system).statsActor(statsConfig.copy(namespace = "monkeys"))

      actor1 must not equal(actor2)
    }
  }
}
