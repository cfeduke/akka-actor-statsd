package deploymentzone.actor

import scala.concurrent.duration._
import org.scalatest.{WordSpecLike, Matchers}
import akka.testkit.ImplicitSender
import akka.actor._
import com.typesafe.config.{ConfigValueFactory, ConfigFactory}
import java.util.concurrent.TimeUnit


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

    "given several messages" when {
      "all messages are queued before the transmitInterval" should {
        "combine all the messages" in new Environment(250) {
          val scheduled = system.actorOf(ScheduledDispatcherActor.props(config, testActor))
          Seq("one", "two", "three", "four").foreach(msg => scheduled ! msg)
          expectMsg(300.milliseconds,
            """one
              |two
              |three
              |four""".stripMargin)
        }
      }
      "some messages are staggered after the transmitInterval" should {
        "receive one batch of messages and then another" in new Environment(50) {
          val scheduled = system.actorOf(ScheduledDispatcherActor.props(config, testActor))
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

  private class Environment(transmitInterval: Long) {
    protected val baseConfig = ConfigFactory.empty()
      .withValue(s"${Config.path}.transmit-interval", ConfigValueFactory.fromAnyRef(transmitInterval))
    lazy val config = Config(baseConfig)
  }

  private class ExceptionCaptureEnvironment(packetSize: Int, transmitInterval: Long)
    extends Environment(transmitInterval) {
    override lazy val config = Config(
      baseConfig.withValue(s"${Config.path}.packet-size",
        ConfigValueFactory.fromAnyRef(packetSize)
      )
    )

    val props = ScheduledDispatcherActor.props(config, system.deadLetters)
    val failureParent = system.actorOf(ExceptionSieve.props(testActor, props))
  }

}
