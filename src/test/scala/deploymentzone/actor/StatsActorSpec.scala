package deploymentzone.actor

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import java.net.InetSocketAddress
import akka.io.Udp
import akka.actor._
import akka.testkit.{TestProbe, ImplicitSender}
import org.scalatest.{WordSpecLike, Matchers}


class StatsSpec
  extends TestKit("stats-spec")
  with WordSpecLike
  with Matchers
  with ImplicitSender {

  "Stats" when {
    "initialized with an empty namespace" should {
      "send the expected message" in new Environment {
        val withoutNamespace = baseConfig.copy(namespace = "")
        val stats = system.actorOf(Stats.props(withoutNamespace), "stats")

        val msg = Increment("dog")
        stats ! msg

        expectMsg(msg.toString)

        shutdown()
      }
    }
    "initialized with a namespace" should {
      "send the expected message" in new Environment {
        val withNamespace = baseConfig.copy(namespace = "name.space")
        val stats = system.actorOf(Stats.props(withNamespace), "stats-ns")
        val msg = Gauge("gauge")(340L)
        stats ! msg
        expectMsg(s"name.space.$msg")

        shutdown()
      }

      "not alter message that is sent over and over" in new Environment {
        val stats = system.actorOf(statsProps, "stats-ns-repeat")
        val msg = Increment("kittens")
        stats ! msg
        expectMsg(msg.toString)
        stats ! msg
        expectMsg(msg.toString)
      }
    }

    "sending multiple messages quickly in sequence" should {
      "transmit all the messages" in new Environment {
        val stats = system.actorOf(statsProps, "stats-mmsg")
        val msgs = Seq(
          Timing("xyz")(40.seconds),
          Increment("ninjas"),
          Decrement("pirates"),
          Gauge("ratchet")(0xDEADBEEF))
        msgs.foreach(stats ! _)

        expectMsg(msgs.mkString("\n").stripLineEnd)

        shutdown()
      }
    }

    "multiple instances" should {
      "all deliver their messages" in new Environment {
        val stats1 = system.actorOf(statsProps, "mi-stats1")
        val stats2 = system.actorOf(statsProps, "mi-stats2")
        val msg1 = Increment("count")
        val msg2 = Decrement("count")
        stats1 ! msg1
        stats2 ! msg2
        expectMsgAllOf(msg1.toString, msg2.toString)
      }
    }
  }

  private class Environment {
    def forwarder = Props(new Actor {
      def receive = { case any => testActor forward any }
    })

    val listener = system.actorOf(UdpListener.props(testActor))
    def boundListenerAddress() = expectMsgClass(classOf[InetSocketAddress])

    val baseConfig = Config(
      ConfigFactory
        .parseResources("StatsSpec.conf")
        .withFallback(ConfigFactory.load))
      .copy(address = boundListenerAddress())
    val statsProps = Stats.props(baseConfig)

    def shutdown() {
      listener ! Udp.Unbind
    }
  }

}
