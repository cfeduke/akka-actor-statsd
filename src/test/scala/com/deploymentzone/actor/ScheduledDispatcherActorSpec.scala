package com.deploymentzone.actor

import org.scalatest.{WordSpecLike, Matchers}
import akka.testkit.ImplicitSender
import scala.concurrent.duration._
import akka.actor._
import akka.actor.SupervisorStrategy.Stop
import akka.actor.OneForOneStrategy

class ScheduledDispatcherActorSpec
  extends TestKit("scheduled-dispatcher-actor-spec")
  with WordSpecLike
  with Matchers
  with ImplicitSender {

  "ScheduledDispatcherActor" when {
    "initialized with a negative packetSize" should {
      "throw an exception" in new ExceptionCaptureEnvironment(-100, 1000) {
        val ex = expectMsgClass(classOf[ActorInitializationException])
        ex.getCause.getMessage should be ("requirement failed: " + ScheduledDispatcherActor.PACKET_SIZE_NEGATIVE_ZERO_MESSAGE)
      }
    }
    "initialized with a zero packetSize" should {
      "throw an exception" in new ExceptionCaptureEnvironment(0, 1000) {
        val ex = expectMsgClass(classOf[ActorInitializationException])
        ex.getCause.getMessage should be ("requirement failed: " + ScheduledDispatcherActor.PACKET_SIZE_NEGATIVE_ZERO_MESSAGE)
      }
    }
    "initialized with a negative transmitInterval" should {
      "throw an exception" in new ExceptionCaptureEnvironment(100, -1000) {
        val ex = expectMsgClass(classOf[ActorInitializationException])
        ex.getCause.getMessage should be ("requirement failed: " + ScheduledDispatcherActor.TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE)
      }
    }
    "initialized with a zero transmitInterval" should {
      "throw an exception" in new ExceptionCaptureEnvironment(100, 0) {
        val ex = expectMsgClass(classOf[ActorInitializationException])
        ex.getCause.getMessage should be ("requirement failed: " + ScheduledDispatcherActor.TRANSMIT_INTERVAL_NEGATIVE_ZERO_MESSAGE)
      }
    }
  }

  private class ExceptionCaptureEnvironment(packetSize: Int, transmitInterval: Long) {
    val props = ScheduledDispatcherActor.props(packetSize, transmitInterval)
    val failureParent = system.actorOf(Props(new Actor with ActorLogging {
      var child: ActorRef = context.actorOf(props)

      override val supervisorStrategy = OneForOneStrategy() {
        case f =>
          testActor ! f
          Stop
      }
      def receive = {
        case msg => child forward msg
      }
    }))
  }
  private class Environment {

  }
}
