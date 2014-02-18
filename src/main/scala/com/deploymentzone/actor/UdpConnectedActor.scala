package com.deploymentzone.actor

import java.net.InetSocketAddress
import akka.actor.{ActorLogging, Props, ActorRef, Actor}
import akka.io.{UdpConnected, IO}
import akka.util.ByteString

/* originated from: http://doc.akka.io/docs/akka/snapshot/scala/io-udp.html */

/* by using a connected form instead of a simple sender, security checks are cached instead of verified on every send */
private[actor] class UdpConnectedActor(remote: InetSocketAddress, requester: ActorRef)
  extends Actor
  with ActorLogging {
  import context.system

  def receive = {
    case UdpConnected.Connect =>
      IO(UdpConnected) ! UdpConnected.Connect(self, remote)
    case UdpConnected.Connected =>
      context.become(ready(sender))
      requester ! UdpConnected.Connected
  }

  def ready(connection: ActorRef): Receive = {
    case msg: String =>
      connection ! UdpConnected.Send(ByteString(msg))
    case d @ UdpConnected.Disconnect => connection ! d
    case UdpConnected.Disconnected   => context.stop(self)
  }
}

private[actor] object UdpConnectedActor {
  def props(remote: InetSocketAddress, requester: ActorRef) = Props(new UdpConnectedActor(remote, requester))
}
