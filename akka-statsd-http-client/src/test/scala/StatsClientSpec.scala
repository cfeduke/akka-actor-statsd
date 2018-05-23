package akka.statsd.http.client

import scala.concurrent.duration._
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.statsd.http.client.StatsClient.ReqRes
import akka.statsd.{Config, Increment, Timing}
import akka.testkit.{TestKitBase, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Assertion, AsyncFunSpecLike, MustMatchers}
import scala.concurrent.Future

class StatsClientSpec extends {
  override implicit val system: ActorSystem = ActorSystem()
} with TestKitBase with AsyncFunSpecLike with MustMatchers {

  implicit val statsConfig =
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

  def withClient(test: (TestProbe, StatsClient) => Future[Assertion]): Future[Assertion] = {
    val probe = TestProbe()

    val client = new StatsClient {
      override protected def statsActor(system: ActorSystem, statsConfig: Config): ActorRef = probe.ref
    }

    test(probe, client)
  }

  describe("Stats Client") {
    it("tracks request counts") {
      withClient { (probe, client) =>
        val makeRequest = client.countRequests(_ => Future.successful(HttpResponse()))

        makeRequest(HttpRequest(uri = "http://www.monkeys.com/foo/bar")).map { _ =>
          probe.expectMsgPF(1.second) {
            case m: Increment =>
              m.bucket.render must equal("http.client.request.get.http.www-monkeys-com.foo.bar")
          }
        }
      }
    }

    it("tracks response counts") {
      withClient { (probe, client) =>
        val makeRequest = client.countResponses(_ => Future.successful(HttpResponse()))

        makeRequest(HttpRequest(uri = "http://www.monkeys.com/foo/bar")).map { _ =>
          probe.expectMsgPF(1.second) {
            case m: Increment =>
              m.bucket.render must equal("http.client.response.get.2xx.http.www-monkeys-com.foo.bar")
          }
        }
      }
    }

    it("tracks response times") {
      withClient { (probe, client) =>
        val makeRequest = client.timeResponses(_ => Future.successful(HttpResponse()))
        makeRequest(HttpRequest(uri = "http://www.monkeys.com/foo/bar")).map { _ =>
          probe.expectMsgPF(1.second) {
            case m: Timing =>
              m.bucket.render must equal("http.client.response.get.2xx.http.www-monkeys-com.foo.bar")
          }
        }
      }
    }

    it("can compose different stats") {
      withClient { (probe, client) =>

        import client._

        val makeRequest =
          Function.chain(
            Seq(
              countResponses(_: ReqRes),
              countRequests(_: ReqRes)
            )
          )(_ => Future.successful(HttpResponse()))

        makeRequest(HttpRequest(uri = "http://www.monkeys.com/foo/bar")).map { _ =>
          probe.expectMsgPF(1.second) {
            case m: Increment =>
              m.bucket.render must equal("http.client.request.get.http.www-monkeys-com.foo.bar")
          }

          probe.expectMsgPF(1.second) {
            case m: Increment =>
              m.bucket.render must equal("http.client.response.get.2xx.http.www-monkeys-com.foo.bar")
          }
        }
      }
    }
  }
}
