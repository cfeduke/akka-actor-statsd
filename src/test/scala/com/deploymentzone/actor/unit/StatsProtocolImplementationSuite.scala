package com.deploymentzone.actor.unit

import com.deploymentzone.actor._
import org.scalatest.FunSuiteLike
import akka.testkit.ImplicitSender
import akka.actor.{Props, ActorRef, Actor}
import akka.io.UdpConnected
import scala.concurrent.duration._

class StatsProtocolImplementationSuite
  extends TestKit("stats-protocol-implementation-suite")
  with FunSuiteLike
  with ImplicitSender {

  test("connects and then relays a message") {
    val stats = system.actorOf(NoOpStatsActor.props(testActor))
    expectMsg(UdpConnected.Connect)
    stats ! UdpConnected.Connected
    val msg = Increment("ninjas")
    stats ! msg
    expectMsg(msg.toString)
  }

  test("stashes messages until connection is established") {
    val stats = system.actorOf(NoOpStatsActor.props(testActor))
    expectMsg(UdpConnected.Connect)
    val firstMsg = Decrement("turtles")
    val secondMsg = Gauge("ninjas")(4000L, 5.0)
    val thirdMsg = Timing("eric.likes.haskell")(9.seconds)
    stats ! firstMsg
    stats ! secondMsg
    stats ! thirdMsg
    stats ! UdpConnected.Connected
    expectMsg(firstMsg.toString)
    expectMsg(secondMsg.toString)
    expectMsg(thirdMsg.toString)

  }

  private class NoOpStatsActor(val connection : ActorRef)
    extends Actor
    with StatsProtocolImplementation {

    override protected def process(msg: CounterMessage[_]) = msg.toString

  }

  private object NoOpStatsActor {
    def props(connection: ActorRef) = Props(new NoOpStatsActor(connection))
  }

}
