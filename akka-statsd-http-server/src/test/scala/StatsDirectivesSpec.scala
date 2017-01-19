package akka.statsd.http.server

import scala.concurrent.duration._
import org.scalatest.{path => _, _}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.statsd._
import akka.http.scaladsl.server._
import Directives._

class StatsDirectivesSpec
  extends {
    override implicit val system = ActorSystem()
  }
  with TestKitBase
  with FunSpecLike
  with ScalatestRouteTest
  with StatsDirectives
  with MustMatchers {

  def actorRefFactory = system

  override val statsConfig =
    Config(ConfigFactory.parseString(
      """
        {
          akka.statsd.hostname = localhost
          akka.statsd.transformations = [
            {
              pattern = "/foo/[a-zA-Z0-9\\-]+/bar"
              into = "foo.[segment].bar"
            }
          ]
        }
      """
    ).withFallback(ConfigFactory.load))

  override val statsSystem = system

  override lazy val stats = testActor

  val getFooBar = (get & path("foo" / "bar")) { complete("ok") }

  describe("count directive") {
    it("tracks request counts") {
      val route = count {
        getFooBar
      }

      Get("/foo/bar") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case m: Increment => m.bucket.render must equal("http.server.get.foo.bar")
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
          case m: Timing => m.bucket.render must equal("http.server.get.foo.bar")
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
        val expectedBucket = scala.collection.Set("http.server.get.foo.bar")
        received.map { case m: Metric[_] => m.bucket.render }.toSet must equal(expectedBucket)
        received.exists(_.isInstanceOf[Increment]) must equal (true)
        received.exists(_.isInstanceOf[Timing]) must equal (true)
      }
    }
    it("Changes teh bucket if specified") {
      val route = countAndTimeInBucket("init.bucket") {
        getFooBar
      }

      Get("/foo/bar") ~>
        route ~>
        check {
          val received = receiveN(2)
          val expectedBucket = scala.collection.Set("init.bucket.get.foo.bar")
          received.map { case m: Metric[_] => m.bucket.render }.toSet must equal(expectedBucket)
          received.exists(_.isInstanceOf[Increment]) must equal (true)
          received.exists(_.isInstanceOf[Timing]) must equal (true)
        }
    }
  }

  describe("Stats directives") {
    it("replace UUID in paths with 'id' token") {
      val route = count {
        path("foo" / "bar" / Segment) { _ => get { complete("ok") } }
      }

      Get("/foo/bar/ca761232-ED42-11ce-BACD-00aa0057b223") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Increment => msg.bucket.render must equal ("http.server.get.foo.bar.[id]")
        }
      }
    }

    it("replace arbitrary segments with 'segment' token in paths matching configured patterns") {
      val route = count {
        get {
          path("foo" / Segment / "bar") { _ => complete("ok") }
        }
      }

      Get("/foo/NL-TNM-11111/bar") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Increment => msg.bucket.render must equal("http.server.get.foo.[segment].bar")
        }
      }
    }
  }
}
