package akka.statsd
package transport

import scala.concurrent.duration._
import org.scalatest.{WordSpecLike, Matchers}
import akka.testkit.ImplicitSender
import akka.actor._
import com.typesafe.config.{ConfigValueFactory, ConfigFactory}


class ScheduledDispatcherSpec
  extends TestKit("scheduled-dispatcher-spec")
  with WordSpecLike
  with Matchers
  with ImplicitSender {

  "ScheduledDispatcher" when {
    "initialized with a negative packetSize" should {
      "throw an exception" in new ExceptionCaptureEnvironment(-100, 1000) {
        val ex = expectMsgClass(classOf[ActorInitializationException])
        ex.getCause.getMessage should be (
          s"requirement failed: ${ScheduledDispatcher.IllegalPacketSize}")
      }
    }
    "initialized with a zero packetSize" should {
      "throw an exception" in new ExceptionCaptureEnvironment(0, 1000) {
        val ex = expectMsgClass(classOf[ActorInitializationException])
        ex.getCause.getMessage should be (
          s"requirement failed: ${ScheduledDispatcher.IllegalPacketSize}")
      }
    }
    "initialized with a negative transmitInterval" should {
      "throw an exception" in new ExceptionCaptureEnvironment(100, -1000) {
        val ex = expectMsgClass(classOf[ActorInitializationException])
        ex.getCause.getMessage should be (
          s"requirement failed: ${ScheduledDispatcher.IllegalTransmitInterval}")
      }
    }
    "initialized with a zero transmitInterval" should {
      "throw an exception" in new ExceptionCaptureEnvironment(100, 0) {
        val ex = expectMsgClass(classOf[ActorInitializationException])
        ex.getCause.getMessage should be (
          s"requirement failed: ${ScheduledDispatcher.IllegalTransmitInterval}")
      }
    }

    "given several messages" when {
      "all messages are queued before the transmitInterval" should {
        "combine all the messages" in new Environment(250) {
          val scheduled = system.actorOf(ScheduledDispatcher.props(config, forwardTo(testActor)))
          Seq("one", "two", "three", "four").foreach(msg => scheduled ! msg)
          expectMsg(300.milliseconds,
            """one
              |two
              |three
              |four""".stripMargin)
        }
        "combine all the messages and send all the necessary payloads if emptyQueueOnFlush is true" in new QueueFlushEnvironment(10, 250) {
          val scheduled = system.actorOf(ScheduledDispatcher.props(config, forwardTo(testActor)))
          Seq("one", "two", "three", "four").foreach(msg => scheduled ! msg)
          receiveN(2, 300.millis) shouldBe Seq("""one
                                                  |two""".stripMargin,
                                               """three
                                                  |four""".stripMargin)
        }
      }
      "some messages are staggered after the transmitInterval" should {
        "receive one batch of messages and then another" in new Environment(50) {
          val scheduled = system.actorOf(ScheduledDispatcher.props(config, forwardTo(testActor)))
          implicit val executionContext = system.dispatcher
          system.scheduler.scheduleOnce(100.milliseconds, scheduled, "three")
          Seq("one", "two").foreach(msg => scheduled ! msg)
          expectMsg(100.milliseconds,
            """one
              |two""".stripMargin)
          expectMsg("three")
        }
      }
    }
  }

  private class Environment(transmitInterval: Long, emptyQueueOnFlush: Boolean = false) {
    def config = Config(
      ConfigFactory
        .load("ScheduledDispatcherSpec.conf")
        .withValue(
          "akka.statsd.transmit-interval",
          ConfigValueFactory.fromAnyRef(transmitInterval)))

    class Forwarder(recipient: ActorRef) extends Actor {
      def receive = {case any => recipient forward any}
    }

    def forwardTo(recipient: ActorRef) = Props(new Forwarder(recipient))
  }

  private class QueueFlushEnvironment(
    packetSize: Int,
    transmitInterval: Long
  ) extends Environment(transmitInterval) {

    override def config = super.config.copy(packetSize = packetSize, emptyQueueOnFlush = true)
  }

  private class ExceptionCaptureEnvironment(
    packetSize: Int,
    transmitInterval: Long
  ) extends Environment(transmitInterval) {

    override def config = super.config.copy(packetSize = packetSize)

    val props = ScheduledDispatcher.props(config, forwardTo(system.deadLetters))
    val failureParent = system.actorOf(ExceptionSieve.props(testActor, props))
  }

}
