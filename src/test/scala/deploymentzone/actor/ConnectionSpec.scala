package deploymentzone.actor

import java.net.InetSocketAddress
import akka.io._
import akka.testkit.ImplicitSender
import akka.actor._
import org.scalatest.fixture.FunSpecLike


class ConnectionSpec
  extends TestKit("connection-spec")
  with FunSpecLike
  with ImplicitSender {

  type FixtureParam = ActorRef

  override def withFixture(test: OneArgTest) = {
    val listener = system.actorOf(UdpListener.props(testActor), "other-end")
    val boundListenerAddress = expectMsgClass(classOf[InetSocketAddress])

    try
      test(system.actorOf(Connection.props(boundListenerAddress, 2)))
    finally
      listener ! Udp.Unbind

  }

  describe("Connection") {
    it("relays plain string messages") { connection =>
      connection ! "msg"
      expectMsg("msg")
    }
  }

}
