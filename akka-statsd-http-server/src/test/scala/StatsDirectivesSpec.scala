package akka.statsd.http.server

import scala.concurrent.duration._
import org.scalatest.{path => _, _}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.statsd._
import akka.http.scaladsl.server._
import Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.stream.scaladsl.Source
import akka.util.ByteString

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

  override def loadStatsConfig() =
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

  override val extractStats: Directive1[ActorRef] = provide(testActor)

  val getFooBar = (get & path("foo" / "bar")) { complete("ok") }

  val getFooBarSlow = (get & path("foo" / "bar")) {
    val s = Source.tick(100.milli, 100.milli, "x").take(5).map(ByteString(_))
    complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, s))
  }

  val getFooBarFail = (get & path("foo" / "bar")) { complete(StatusCodes.BadRequest) }

  val getFooBarError = (get & path("foo" / "bar")) {
    complete {
      new RuntimeException("Ooooopsie")
    }
  }

  describe("countResponse directive") {
    it("tracks response counts") {
      val route = countResponse {
        getFooBar
      }

      Get("/foo/bar") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case m: Increment => m.bucket.render must equal("http.server.response.get.2xx.foo.bar")
        }
      }
    }

    it("tracks response counts when returning an error") {
      val route = countResponse {
        getFooBarFail
      }

      Get("/foo/bar") ~>
        route ~>
        check {
          expectMsgPF(1.second) {
            case m: Increment => m.bucket.render must equal("http.server.response.get.4xx.foo.bar")
          }
        }
    }

    it("tracks request counts when an exception occurs") {
      val route = countResponse {
        getFooBarError
      }

      Get("/foo/bar") ~>
        route ~>
        check {
          expectMsgPF(1.second) {
            case m: Increment => m.bucket.render must equal("http.server.response.get.exception.foo.bar")
          }
        }
    }
  }

  describe("countRequest directive") {
    it("tracks request counts") {
      val route = countRequest {
        getFooBar
      }

      Get("/foo/bar") ~>
        route ~>
        check {
          expectMsgPF(1.second) {
            case m: Increment => m.bucket.render must equal("http.server.request.get.foo.bar")
          }
        }
    }
  }

  describe("time directive") {
    it("tracks request execution time") {
      val route = time {
        getFooBarSlow
      }

      Get("/foo/bar") ~>
      route ~>
      check {
        status mustEqual StatusCodes.OK  // make sure we actually retrieve the response
        expectMsgPF(1.second) {
          case m: Timing =>
            m.bucket.render must equal("http.server.response.get.2xx.foo.bar")
            m.value must be > 500L
        }
      }
    }
  }

  describe("Stats directives") {
    it("Changes the bucket if specified") {
      val route = countAndTimeInBucket("init.bucket") {
        getFooBar
      }

      Get("/foo/bar") ~>
        route ~>
        check {
          status mustEqual StatusCodes.OK  // make sure we actually retrieve the response
          val received = receiveN(3)
          val expectedBucket = scala.collection.Set(
            "init.bucket.request.get.foo.bar",
            "init.bucket.response.get.2xx.foo.bar"
          )
          received.map { case m: Metric[_] => m.bucket.render }.toSet must equal(expectedBucket)
          received.exists(_.isInstanceOf[Increment]) must equal (true)
          received.exists(_.isInstanceOf[Timing]) must equal (true)
        }
    }

    it("replace UUID in paths with 'id' token") {
      val route = countResponse {
        path("foo" / "bar" / Segment) { _ => get { complete("ok") } }
      }

      Get("/foo/bar/ca761232-ED42-11ce-BACD-00aa0057b223") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Increment => msg.bucket.render must equal ("http.server.response.get.2xx.foo.bar.[id]")
        }
      }
    }

    it("replace arbitrary segments with 'segment' token in paths matching configured patterns") {
      val route = countResponse {
        get {
          path("foo" / Segment / "bar") { _ => complete("ok") }
        }
      }

      Get("/foo/NL-TNM-11111/bar") ~>
      route ~>
      check {
        expectMsgPF(1.second) {
          case msg: Increment => msg.bucket.render must equal("http.server.response.get.2xx.foo.[segment].bar")
        }
      }
    }
  }
}
