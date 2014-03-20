package deploymentzone.actor.unit

import deploymentzone.actor._
import org.scalatest.{WordSpecLike, Matchers}
import akka.testkit.{ImplicitSender, TestActorRef}
import com.typesafe.config.ConfigFactory
import akka.actor.ActorInitializationException

class StatsActorSpec
  extends TestKit("stats-actor-unit-spec")
  with WordSpecLike
  with Matchers
  with ImplicitSender {
  "StatsActor" when {
    "using configuration-only props" should {
      "fall back to the default port when its not specified" in {
        pending // upgrade to akka 2.3
        val config = ConfigFactory.load("just-hostname.conf")
        val subject = TestActorRef[StatsActor](StatsActor.props(config))
        subject.underlyingActor.address.getPort should be(Defaults.STATSD_UDP_PORT)
      }
      "throw an exception when no hostname is specified" in {
        pending // upgrade to akka 2.3
        val config = ConfigFactory.empty()
        val props = StatsActor.props(config)
        system.actorOf(ExceptionSieve.props(props))
        val subject = expectMsgClass(classOf[ActorInitializationException])
        subject.getCause.getMessage should startWith("No configuration setting found for key 'deploymentzone")
      }
      "get initialized with the expected values when all values are specified" in {
        pending // upgrade to akka 2.3
        val config = ConfigFactory.load("stats-actor.conf")
        val subject = TestActorRef[StatsActor](StatsActor.props(config))
        subject.underlyingActor.address.getAddress.getHostAddress should be("127.0.0.1")
        subject.underlyingActor.address.getPort should be(9999)
        subject.underlyingActor.namespace should be("mango")
      }
    }

    "using empty props" should {
      "load the expected props" in {
        pending // upgrade to akka 2.3
        val subject = TestActorRef[StatsActor](StatsActor.props())
        subject.underlyingActor.address.getAddress.getHostAddress should be("127.0.0.1")
        subject.underlyingActor.address.getPort should be(32768)
        subject.underlyingActor.namespace should be("")
      }
    }

    "using another constructor" should {
      "override the default settings with only the provided values" in {
        pending // upgrade to akka 2.3
        val subject = TestActorRef[StatsActor](StatsActor.props("127.0.0.1", "a-namespace"))
        subject.underlyingActor.address.getAddress.getHostAddress should be("127.0.0.1")
        subject.underlyingActor.address.getPort should be(32768)
        subject.underlyingActor.namespace should be("a-namespace")
      }
    }
  }

}
