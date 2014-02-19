package com.deploymentzone.actor.integration

import com.deploymentzone.actor.{StatsActor, UdpListenerActor, TestKit}
import org.scalatest.{WordSpecLike, Matchers}
import java.net.InetSocketAddress
import com.deploymentzone.actor.protocol.{Timing, Gauge, Increment}
import akka.testkit.{TestProbe, ImplicitSender}
import akka.io.Udp
import akka.actor.Terminated
import scala.concurrent.duration._

class StatsActorSpec
  extends TestKit("stats-actor-suite")
  with WordSpecLike
  with Matchers
  with ImplicitSender {

  "StatsActor" when {
    "initialized without a namespace" should {
      "send the expected message" in new Environment {
        val stats = system.actorOf(StatsActor.props(address), "stats")
        val msg = Increment("dog")
        stats ! msg
        expectMsg(msg.toString)

        shutdown()
      }
    }
    "initialized with a namespace" should {
      "send the expected message" in new Environment {
        val stats = system.actorOf(StatsActor.props(address, "name.space"), "stats-ns")
        val msg = Gauge("gauge")(340L)
        stats ! msg
        expectMsg(s"name.space.$msg")

        shutdown()
      }
    }
    "initialized with a null address" should {
      "throw an exception" in new Environment {
        val probe = TestProbe()
        probe watch system.actorOf(StatsActor.props(null))
        probe expectMsgClass classOf[Terminated]

        shutdown()
      }
    }
    "initialized with a null namespace" should {
      "be permitted" in new Environment {
        val stats = system.actorOf(StatsActor.props(address, null), "stats-null-ns")
        val msg = Timing("xyz")(40.seconds)
        stats ! msg
        expectMsg(msg.toString)

        shutdown()
      }
    }
  }

  private class Environment {
    val listener = system.actorOf(UdpListenerActor.props(testActor), "listener")
    val address = expectMsgClass(classOf[InetSocketAddress])

    def shutdown() {
      listener ! Udp.Unbind
    }
  }

}
