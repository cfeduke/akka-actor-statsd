# akka-statsd
[![Build Status](https://travis-ci.org/NewMotion/akka-statsd.svg?branch=master)](https://travis-ci.org/NewMotion/akka-statsd)

A dead simple [statsd] client written in Scala as group of actors using the [akka] framework.

## Installation

Releases and snapshots are hosted on The New Motion public repository. To add dependency to your project use following snippet:

```scala
libraryDependencies += "com.newmotion" %% "akka-statsd-core" % "2.0.0"
```

For stats collection over HTTP requests served by akka-http server add:
```scala
libraryDependencies += "com.newmotion" %% "akka-statsd-http-server" % "2.0.0"
```

## Configuration

By default `Stats.props()` uses `Config` which is constructed by resolving Typesafe Config with call to `ConfigFactory.load()`.
You may use `akkas.statsd.Config`, constructed manually or from provided Typesafe Config instance too. 
Here are the default settings, with the exception of hostname, which is a required setting:

```
akka.statsd {
        # hostname = "required"
        port = 8125
        namespace = ""
        # common packet sizes:
        # gigabit ethernet:     8932
        # fast ethernet:        1432
        # commodity internet:    512
        packet-size = 1432
        transmit-interval = 100 ms

        transformations = [
          {
            pattern = "foo"
            into = "bar"
          }
        ]
    }
}
```

## Simplest Example

Following code can be executed as is in console of your project if library included as dependency
```scala
def sendStats() {
  import akka.actor.ActorSystem
  import akka.statsd._
  import com.typesafe.config.ConfigValueFactory.fromAnyRef
 
  val system = ActorSystem("Example")
  /* hostname is only one setting that has no default */
  val cfg = Config(system.settings.config.withValue("akka.statsd.hostname", fromAnyRef("localhost")))
  lazy val stats = system.actorOf(Stats.props(cfg))
  stats ! Increment(Bucket("my.thingie.that.counts.app.starts"))

  system.terminate()
}
```

or (if you don't want to use actor's directly)

```scala
def sendStats() {
  import concurrent.duration._
  import akka.actor.ActorSystem
  import akka.statsd._
  import com.typesafe.config.ConfigValueFactory.fromAnyRef


  val system = ActorSystem("Example")
  /* hostname is only one setting that has no default */
  val cfg = Config(system.settings.config.withValue("akka.statsd.hostname", fromAnyRef("localhost")))
  implicit val statsActor = system.actorOf(Stats.props(cfg))

  Stats.increment(Bucket("endpoint1"))
  Stats.time(Bucket("page2.response"))(250.milliseconds)

  Stats.withTimer(Bucket("code.execution.time")) {
      //execute code here
      Thread.sleep(new util.Random().nextInt(5000))
  }

  system.terminate()
}
```
## Naming conventions

It is recommended to use the following naming conventions:

A 'bucket' consists of a namespace part and a hierarchy part

The namespace is set by adding `[environment].[application]` in the application.conf

environment can be `test`, `sandbox`, `prod` and even `unittest`
the application a one or two word description of the application separated by dashes

Think about the hierarchy you want to use inside your application, this will be of great influence on the representation side

## More Examples

```scala
class Counter(val counterName: String) extends Actor {
  var seconds = 0L
  var minutes = 0L
  var hours = 0L

  lazy val stats = context.actorOf(Stats.props())
  val secondsCounter = Increment(Bucket("seconds"))
  val minutesCounter = Increment(Bucket("minutes"))
  val hoursCounter = Increment(Bucket("hours"))
  val gaugeMetric = Gauge(Bucket("shotgun"))(12L)
  val timingMetric = Timing(Bucket("tempo"))(5.seconds)

  def receive = {
    case IncrementSecond =>
      seconds += 1
      statsd ! secondsCounter
    case IncrementMinute =>
      minutes += 1
      statsd ! gaugeMetric
      statsd ! minutesCounter
      statsd ! timingMetric
    case IncrementHour =>
      hours += 1
      statsd ! hoursCounter
    case Status => 
      sender ! (seconds, minutes, hours)
  }
}
```

The counter messages themselves are curried for convenience:

```scala
val interval = Timing(Bucket("interval"))
stats ! interval(3.seconds)
stats ! interval(2.seconds)
stats ! interval(1.second)
```

If a `Stats` instance is assigned a namespace(via the Config object) then all counters sent from that actor have the namespace applied to the counter:

```scala
val page1Perf = system.actorOf(Stats.props(<config_with_namespace>))
val page2Perf = system.actorOf(Stats.props(<config_with_namespace>))
val response = Timing(Bucket("response"))
page1Perf ! response(250.milliseconds)
page2Perf ! response(100.milliseconds)
```

But you don't have to use namespaced actors at all:

```scala
val perf = system.actorOf(Stats.props())
perf ! Timing(Bucket("page1.response"))(250.milliseconds)
perf ! Timing(Bucket("page2.response"))(100.milliseconds)
```

## Explanation

This implementation is intended to be of high performance, thus it

- uses an akka.io UdpConnected implementation to cache local security checks within the JVM if SecurityManager is enabled
- allows for message batching before sending them out to StatsD, as per [statsd Multi-Metric Packets](https://github.com/etsy/statsd/blob/master/docs/metric_types.md#multi-metric-packets) specification. Turned on by default

Supports all of the [StatsD Metric Types](https://github.com/etsy/statsd/blob/master/docs/metric_types.md) including optional sampling parameters.

| StatsD               | 
-statsd       |
|:---------------------|:------------------------|
| Counting             | Count                   |
| Timing               | Timing                  |
| Gauges               | Gauge                   |
| Gauge (modification) | GaugeAdd, GaugeSubtract |
| Sets                 | Set                     |

### Batching

As messages are transmitted to a stats actor those messages are queued for later transmission. By default the queue flushes every 100 milliseconds and combines messages together up to a packet size of 1432 bytes (taking UTF-8 character sizes into account).

This can be turned off in the configuration by setting `enable-multi-metric = false`

## Bucket transformations

Any `UUID` in the bucket path substituted with token `[id]`. In addition to this, clients can specify custom transformations to influence the way bucket names are augmented before being sent to StatsD server.

In order to do so, the client configuration should contain `akka.statsd.transformations` section of the following format:

```
akka.statsd.transformations = [
  {
    pattern = "/foo/[a-z0-9]+/bar",
    into    = "/foo/[segment]/bar"
  }
]
```

This reads as: if the part of the bucket matches the regular expression in `pattern`, replace it with the one described in `into` (which is *not* a regular expression).

Please note that:
- client-defined transformations take priority over `UUID` replacement
- transformations are matched from top to bottom
- every transformation that matches the path will be applied


## Compatibility

Since version `0.9.0`, if typesafe configuration used, provide settings in namespace `akka.statsd`.

Versions before `0.9.0` use `deploymentzone.statsd`

## Influences

Forked from githup repo at [cfeduke/akka-actor-statsd](https://github.com/cfeduke/akka-actor-statsd)

