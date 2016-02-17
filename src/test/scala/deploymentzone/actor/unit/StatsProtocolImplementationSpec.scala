package deploymentzone.actor
package unit

import scala.concurrent.duration._
import deploymentzone.actor._
import org.scalatest.FunSpecLike
import akka.testkit.ImplicitSender
import akka.actor.{Props, ActorRef, Actor}
import akka.io.UdpConnected
import com.typesafe.config.ConfigFactory


class StatsProtocolImplementationSpec
  extends TestKit("stats-protocol-implementation-spec")
  with FunSpecLike
  with ImplicitSender {

  describe("StatsProtocolImplementation") {
    it("relays a message when connected") {
      val stats = system.actorOf(NoOpStatsActor.props(testActor))
      expectMsg(UdpConnected.Connect)
      stats ! UdpConnected.Connected
      val msg = Increment("ninjas")
      stats ! msg
      expectMsg(msg.toString)
    }

    it("stashes messages until connection is established") {
      val stats = system.actorOf(NoOpStatsActor.props(testActor))
      expectMsg(UdpConnected.Connect)
      val msgs = Seq(
        Decrement("turtles"),
        Gauge("ninjas", 5.0)(4000L),
        Timing("eric.likes.haskell")(9.seconds))
      msgs.foreach(msg => stats ! msg)
      expectNoMsg(2.seconds)
      stats ! UdpConnected.Connected
      expectMsg(msgs.mkString("\n").stripLineEnd)
    }
  }

  private class NoOpStatsActor(val connection : ActorRef)
    extends Actor
    with StatsProtocolImplementation {

    override protected[this] val config =
      Config(ConfigFactory.load("StatsProtocolImplementationSpec.conf"))

    override protected def process(msg: Metric[_]) = msg.toString

  }

  private object NoOpStatsActor {
    def props(connection: ActorRef) = Props(new NoOpStatsActor(connection))
  }

}
