package deploymentzone.actor.integration

import deploymentzone.actor.{UdpListenerActor, TestKit, UdpConnectedActor}
import org.scalatest.FunSuiteLike
import akka.testkit.ImplicitSender
import java.net.InetSocketAddress
import akka.io.{UdpConnected, Udp}

class UdpConnectedActorSuite
  extends TestKit("udp-connected-actor-suite")
  with FunSuiteLike
  with ImplicitSender {

  test("sends data") {
    val listener = system.actorOf(UdpListenerActor.props(testActor))
    val address = expectMsgClass(classOf[InetSocketAddress])
    val connected = system.actorOf(UdpConnectedActor.props(address, testActor))
    connected ! UdpConnected.Connect
    expectMsg(UdpConnected.Connected)
    connected ! "data"
    expectMsg("data")
    connected ! UdpConnected.Disconnect
    listener ! Udp.Unbind
  }

}
