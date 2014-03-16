package deploymentzone.actor

import org.scalatest.{BeforeAndAfterAll, Suite}
import akka.actor._
import akka.actor.SupervisorStrategy.Stop

trait BeforeAndAfterAllTestKit
  extends Suite with BeforeAndAfterAll {

  implicit def system: ActorSystem

  override protected def afterAll() {
    akka.testkit.TestKit.shutdownActorSystem(system)
  }

}

abstract class TestKit(actorSystemName: String)
  extends akka.testkit.TestKit(ActorSystem(actorSystemName))
  with BeforeAndAfterAllTestKit { this: Suite =>

  class ExceptionSieve(private val testActor: ActorRef, val supervisedChildProps: Props)
    extends Actor {
    var child: ActorRef = context.actorOf(supervisedChildProps)
    override val supervisorStrategy = OneForOneStrategy() {
      case f =>
        testActor ! f
        Stop
    }
    def receive = {
      case msg => child forward msg
    }
  }

  object ExceptionSieve {
    def props(testActor: ActorRef, supervisedChildProps: Props): Props =
      Props(new ExceptionSieve(testActor, supervisedChildProps))
    def props(supervisedChildProps: Props)(implicit testActor: ActorRef): Props =
      Props(new ExceptionSieve(testActor, supervisedChildProps))
  }
}


