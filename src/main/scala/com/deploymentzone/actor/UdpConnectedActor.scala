package com.deploymentzone.actor

import java.net.InetSocketAddress
import akka.actor.{ActorRef, Actor}
import akka.io.{UdpConnected, IO}
import akka.util.ByteString

/* originated from: http://doc.akka.io/docs/akka/snapshot/scala/io-udp.html */
private[actor] class UdpConnectedActor(remote: InetSocketAddress) extends Actor {
  import context.system
  IO(UdpConnected) ! UdpConnected.Connect(self, remote)

  def receive = {
    case UdpConnected.Connected =>
      context.become(ready(sender))
  }

  def ready(connection: ActorRef): Receive = {
    case UdpConnected.Received(data) =>
    // process data, send it on, etc.
    case msg: String =>
      connection ! UdpConnected.Send(ByteString(msg))
    case d @ UdpConnected.Disconnect => connection ! d
    case UdpConnected.Disconnected   => context.stop(self)
  }
}
