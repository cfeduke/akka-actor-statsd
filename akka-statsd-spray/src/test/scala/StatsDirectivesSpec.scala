package deploymentzone.spray

import scala.concurrent.duration._
import org.scalatest._
import akka.testkit._
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorRef, ActorSystem, ActorRefFactory}
import spray.http.StatusCodes
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest
import deploymentzone.actor.{Metric, Increment, Timing, Stats}


class StatsDirectivesSpec
  extends {
    override implicit val system = ActorSystem()
  }
  with TestKitBase
  with FunSpecLike
  with ScalatestRouteTest
  with StatsDirectives
  with HttpService
  with Matchers {

  def actorRefFactory = system

  val configString = """
    {
      akka.statsd.hostname = localhost
      akka.statsd.transform = [
        {
          pattern = "/foo/[a-zA-Z0-9\\-]+/bar"
          into = "/foo/[segment]/bar"
        }
      ]
    }
  """

  override val statsConfig =
    deploymentzone.Config(
      ConfigFactory.parseString(configString).withFallback(ConfigFactory.load))

  override val statsSystem = system

  statsActors.update(statsSystem.name, testActor)

  val getFooBar = (get & path("foo" / "bar")) {
    complete("ok")
  }

  describe("count directive") {
    it("tracks request counts") {
      val route = count {
        getFooBar
      }

      Get("/foo/bar") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Increment => msg.bucket === "get.foo.bar"
        }
      }
    }
  }

  describe("time directive") {
    it("tracks request execution time") {
      val route = time {
        getFooBar
      }

      Get("/foo/bar") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Timing => msg.bucket === "get.foo.bar"
        }
      }
    }
  }

  describe("countAndTime directive") {
    it("tracks both request counts and execution time") {
      val route = countAndTime {
        getFooBar
      }

      Get("/foo/bar") ~>
      route ~>
      check {
        val received = receiveN(2)
        val expectedBucket = "get.foo.bar"
        received.map { case m: Metric[_] => m.bucket }.toSet === Set(expectedBucket)
        received.exists(_.isInstanceOf[Increment]) === true
        received.exists(_.isInstanceOf[Timing]) === true
      }
    }
  }

  describe("Stats directives") {
    it("replace UUID in paths with 'id' token") {
      val route = count {
        path("foo" / "bar" / Segment) { id =>
          get {
            complete("ok")
          }
        }
      }

      Get("/foo/bar/ca761232-ED42-11ce-BACD-00aa0057b223") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Increment => msg.bucket === "get.foo.bar.[id]"
        }
      }
    }

    it("replace arbitrary segments with 'segment' token in paths matching configured patterns") {
      val route = count {
        path("foo" / Segment / "bar") { segment =>
          get {
            complete("ok")
          }
        }
      }

      Get("/foo/NL-TNM-11111/bar") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Increment => msg.bucket === "get.foo.[segment].bar"
        }
      }
    }

    it("favor configured transormation patterns over default") {
      val route = count {
        path("foo" / Segment / "bar") { segment =>
          get {
            complete("ok")
          }
        }
      }

      Get("/foo/ca761232-ED42-11ce-BACD-00aa0057b223/bar") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Increment => msg.bucket === "get.foo.[segment].bar"
        }
      }
    }
  }
}
