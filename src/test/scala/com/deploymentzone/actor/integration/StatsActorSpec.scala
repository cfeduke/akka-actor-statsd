package com.deploymentzone.actor.integration

import com.deploymentzone.actor.{StatsActor, UdpListenerActor, TestKit}
import org.scalatest.{WordSpecLike, WordSpec, FunSuiteLike, Matchers}
import java.net.InetSocketAddress
import com.deploymentzone.actor.protocol.Increment
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

        listener ! Udp.Unbind
      }
    }
  }

  private class Environment {
    val listener = system.actorOf(UdpListenerActor.props(testActor), "listener")
    val address = expectMsgClass(classOf[InetSocketAddress])
  }

}
