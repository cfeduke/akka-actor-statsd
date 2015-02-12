package deploymentzone.actor.unit

import deploymentzone.actor._
import org.scalatest.FunSuiteLike
import akka.testkit.ImplicitSender
import akka.actor.{Props, ActorRef, Actor}
import akka.io.UdpConnected
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory

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

//  test("stashes messages until connection is established") {
//    val stats = system.actorOf(NoOpStatsActor.props(testActor))
//    expectMsg(UdpConnected.Connect)
//    val msgs = Seq(Decrement("turtles"),
//                   Gauge("ninjas", 5.0)(4000L),
//                   Timing("eric.likes.haskell")(9.seconds))
//    msgs.foreach(msg => stats ! msg)
//    stats ! UdpConnected.Connected
//    expectMsg(msgs.mkString("\n").stripLineEnd)
//  }

  private class NoOpStatsActor(val connection : ActorRef)
    extends Actor
    with StatsProtocolImplementation {

    override protected[this] val config = Config(ConfigFactory.load())

    override protected def process(msg: Metric[_]) = msg.toString

  }

  private object NoOpStatsActor {
    def props(connection: ActorRef) = Props(new NoOpStatsActor(connection))
  }

}
