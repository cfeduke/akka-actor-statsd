package com.deploymentzone.actor.integration

import com.deploymentzone.actor.{StatsActor, UdpListenerActor, TestKit}
import org.scalatest.{WordSpecLike, WordSpec, FunSuiteLike, Matchers}
import java.net.InetSocketAddress
import com.deploymentzone.actor.protocol.{Gauge, Increment}
import akka.testkit.ImplicitSender
import akka.io.Udp

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
  }

  private class Environment {
    val listener = system.actorOf(UdpListenerActor.props(testActor), "listener")
    val address = expectMsgClass(classOf[InetSocketAddress])

    def shutdown() {
      listener ! Udp.Unbind
    }
  }

}
