package akka.statsd.http.client

import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.statsd.{Config, Increment, Timing}
import akka.testkit.{TestKitBase, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{AsyncFunSpecLike, MustMatchers}
import scala.concurrent.Future

class StatsClientSpec extends {
  override implicit val system: ActorSystem = ActorSystem()
} with TestKitBase with AsyncFunSpecLike with MustMatchers {

  class TestClient(
    probe: TestProbe,
    singleRequest: HttpRequest => Future[HttpResponse]
  ) extends StatsClient(singleRequest) {

    override val statsConfig =
      Config(
        ConfigFactory
          .parseString(
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
          )
          .withFallback(ConfigFactory.load)
      )

    override def createStatsActor(): ActorRef = probe.ref
  }

  describe("Stats Client") {
    it("tracks request counts") {
      val probe = TestProbe()
      val client = new TestClient(probe, _ => Future.successful(HttpResponse())) with CountRequests
      client.singleRequest(HttpRequest(uri = "http://www.monkeys.com/foo/bar")).map { _ =>
        probe.expectMsgPF(1.second) {
          case m: Increment =>
            m.bucket.render must equal("http.client.request.get.http.www-monkeys-com.foo.bar")
        }
      }
    }

    it("tracks response counts") {
      val probe = TestProbe()
      val client = new TestClient(probe, _ => Future.successful(HttpResponse())) with CountResponses
      client.singleRequest(HttpRequest(uri = "http://www.monkeys.com/foo/bar")).map { _ =>
        probe.expectMsgPF(1.second) {
          case m: Increment =>
            m.bucket.render must equal("http.client.response.get.2xx.http.www-monkeys-com.foo.bar")
        }
      }
    }

    it("tracks response times") {
      val probe = TestProbe()
      val client = new TestClient(probe, _ => Future.successful(HttpResponse())) with TimeResponses
      client.singleRequest(HttpRequest(uri = "http://www.monkeys.com/foo/bar")).map { _ =>
        probe.expectMsgPF(1.second) {
          case m: Timing =>
            m.bucket.render must equal("http.client.response.get.2xx.http.www-monkeys-com.foo.bar")
        }
      }
    }
  }
}
