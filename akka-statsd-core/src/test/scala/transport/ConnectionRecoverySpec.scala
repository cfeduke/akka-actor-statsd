package akka.statsd
package transport

import akka.actor._

import org.scalatest._
import akka.testkit.ImplicitSender
import Connection._


class ConnectionRecoverySpec
  extends TestKit("connection-recovery-spec")
  with FunSpecLike
  with ImplicitSender {

  def expectConnectionOpened = expectMsg(Opened)
  def expectAllDelivered(msgs: String*) = expectMsgAllOf(msgs.map("Success "+_): _*)
  def expectDeliveryFailed(msgs: String*) = expectMsgAllOf(msgs.map("Failure "+_): _*)

  describe("Connection") {
    it("restarts anew after certain number of message delivery failures") {
      val failures = 2

      class Flakey(failAfter: Int) extends Actor {
        var tillFailure = failAfter
        def receive = {
          case Deliver(msg) =>
            tillFailure -= 1
            if (tillFailure < 0) {
              sender() ! DeliveryFailed(msg)
              testActor ! s"Failure $msg"
            } else
              testActor ! s"Success $msg"
          case Open =>
            sender() ! Opened
            testActor ! Opened
        }
      }

      val messages = (1 to 4).map(_.toString)

      val connection = system.actorOf(Props(new Connection(Props(new Flakey(failures)), 1)))
      expectConnectionOpened

      connection ! "1st successful"
      connection ! "2nd successful"
      expectAllDelivered("1st successful", "2nd successful")

      connection ! "3rd"
      expectDeliveryFailed("3rd")

      connection ! "4th"
      expectConnectionOpened

      expectAllDelivered("4th")
      connection ! "1st after reconnection"

      expectAllDelivered("1st after reconnection")
    }
  }



}
