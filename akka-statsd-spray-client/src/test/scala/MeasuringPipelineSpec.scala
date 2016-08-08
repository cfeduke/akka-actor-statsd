package akka.statsd.spray.client

import org.scalatest.fixture._
import org.scalatest.MustMatchers
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.testkit._
import spray.http._
import spray.client.pipelining._
import akka.statsd.{Config => StatsConfig, _}


class MeasuringPipelineSpec
  extends TestKit(ActorSystem())
  with DefaultTimeout
  with FunSpecLike
  with MustMatchers {

  type FixtureParam = Ctx

  case class Ctx(
    pipeline: SendReceive,
    statistician: TestProbe,
    transport: TestProbe
  )

  implicit val sm = system
  import sm.dispatcher
  implicit val requestTimeout = akka.util.Timeout(1.second)

  val request = HttpRequest(HttpMethods.GET, Uri("http://www.example.com/apen/schaap"))
  val response = HttpResponse()

  override def withFixture(test: OneArgTest) = {
    val transport = TestProbe()
    val stat = TestProbe()

    val sut = new MeasuringPipeline {
      val statistician = stat.ref
      override val statsConfig = StatsConfig(ConfigFactory.parseString(
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
    }

    test(Ctx(sut.measuring(sendReceive(transport.ref)), stat, transport))
  }

  describe("Measuring pipeline") {
    it("provides number and timing of requests") { ctx =>
      import ctx._

      pipeline(request)
      transport.expectMsg(request)
      transport.reply(response)

      val expected = "http.client.success.get.www.example.com.apen.schaap"

      statistician.expectMsgPF() {
        case t: Timing => t.bucket.render must equal(expected)
      }
      statistician.expectMsgPF() {
        case i: Increment => i.bucket.render must equal(expected)
      }
    }

    it("provides number and timing of failed requests") { ctx =>
      import ctx._
      pipeline(request)

      transport.expectMsg(request)
      transport.reply(response.copy(status = StatusCodes.NotFound))

      val expected = "http.client.failure.404.get.www.example.com.apen.schaap"

      statistician.expectMsgPF() {
        case t: Timing => t.bucket.render must equal(expected)
      }

      statistician.expectMsgPF() {
        case i: Increment => i.bucket.render must equal(expected)
      }
    }

    it("treats timeouts as errors") { ctx =>
      import ctx._
      pipeline(request)

      transport.expectMsg(request)

      statistician.expectMsgPF(requestTimeout.duration + 1.second) {
        case i: Increment => i.bucket.render must equal("http.client.error.get.www.example.com.apen.schaap")
      }
    }

    it("includes port number in bucket name if it is non-standard") { ctx =>
      import ctx._
      val weirdPortRequest = request.copy(uri = "https://www.example.com:705/")
      pipeline(weirdPortRequest)
      transport.expectMsg(weirdPortRequest)
      transport.reply(response)

      statistician.expectMsgPF() {
        case t: Timing => t.bucket.render must equal("http.client.success.get.www.example.com.705")
      }
    }
  }
}
