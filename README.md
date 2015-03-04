# akka-actor-statsd

A dead simple [statsd] client written in Scala as an actor using the [akka] framework.

## naming conventions

For The New Motion applications use the following naming conventions:

A 'bucket' consists of a namespace part and a hierarchy part

the namespace is set by adding `[environment].[application]` in the application.conf

environment can be `test` `sandbox` `prod` and even `unittest`
the application a one or two word description of the application separated by dashes

Think about the hierarchy you want to use inside your application, this will be of great influence on the representation side

## Examples

```scala
class Counter(val counterName: String) extends Actor {
  var seconds = 0L
  var minutes = 0L
  var hours = 0L

  lazy val stats = context.actorOf(StatsActor.props("stats.someserver.com", s"prototype.$counterName"))
  val secondsCounter = Increment("seconds")
  val minutesCounter = Increment("minutes")
  val hoursCounter = Increment("hours")
  val gaugeMetric = Gauge("shotgun")(12L)
  val timingMetric = Timing("tempo")(5.seconds)

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
    case Status => sender ! (seconds, minutes, hours)
  }
}
```

The counter messages themselves are curried for convience:

```scala
val interval = Timing("interval")
stats ! interval(3.seconds)
stats ! interval(2.seconds)
stats ! interval(1.second)
```

If a `StatsActor` instance is assigned a namespace then all counters sent from that 
actor have the namespace applied to the counter:

```scala
val page1Perf = system.actorOf(StatsActor.props("stats.someserver.com", "page1"))
val page2Perf = system.actorOf(StatsActor.props("stats.someserver.com", "page2"))
val response = Timing("response")
page1Perf ! response(250.milliseconds)
page2Perf ! response(100.milliseconds)
```

But you don't have to use namespaced actors at all:

```scala
val perf = system.actorOf(StatsActor.props("stats.someserver.com"))
perf ! Timing("page1.response")(250.milliseconds)
perf ! Timing("page2.response")(100.milliseconds)
```

### API

There is a simple api if you dont want to use the actor's ! method's

```scala
implicit val statsActor = context.actorOf(StatsActor.props("stats.someserver.com"))

Stats.increment("endpoint1")
Stats.timing("page2.response")(250 milliseconds)

Stats.withTimer("code.execution.time) {
    //execute code here
    Thread.sleep(new Random().nextInt())
}
```



## Installation

Releases are hosted on Maven Central.

```scala
libraryDependencies ++= Seq("com.deploymentzone" %% "akka-actor-statsd" % "0.1.2")
```

Snapshots are hosted on The New Motion public repository.

```scala
resolvers += "The New Motion Public Repo" at "http://nexus.thenewmotion.com/content/groups/public/"
```

This library requires [Akka](http://akka.io) 2.3 to get around a bug with 
[Stash and TestActorRef](http://stackoverflow.com/questions/21725473/creating-a-testactorref-results-in-nullpointerexception/22432436#22432436) for test purposes only. It is compatible with Akka 2.2.3. If you need to keep a dependency on Akka 2.2.3 (for use with [scala-redis-nb](https://github.com/debasishg/scala-redis-nb/tree/master) for example) be sure to use an exclusion rule:

```scala
libraryDependencies ++= Seq("com.deploymentzone" %% "akka-actor-statsd" % "0.1.2"
  excludeAll ExclusionRule("com.typesafe.akka"))
```

## Explanation

This implementation is intended to be very high performance.

- Uses an akka.io UdpConnected implementation to bypass local security checks within the JVM
- Batches messages together, as per [statsd Multi-Metric Packets](https://github.com/etsy/statsd/blob/master/docs/metric_types.md#multi-metric-packets) specification

Supports all of the [statsd Metric Types](https://github.com/etsy/statsd/blob/master/docs/metric_types.md) including
optional sampling parameters.

| statsd               | akka-actor-statsd       |
|:---------------------|:------------------------|
| Counting             | Count                   |
| Timing               | Timing                  |
| Gauges               | Gauge                   |
| Gauge (modification) | GaugeAdd, GaugeSubtract |
| Sets                 | Set                     |

### Batching

As messages are transmitted to a stats actor those messages are queued for later 
transmission. By default the queue flushes every 100 milliseconds and combines messages
together up to a packet size of 1,432 bytes (taking UTF-8 character sizes into account).

This can be turned off in the configuration by setting `enable-multi-metric = false`


## Configuration

You may pass your own Typesafe Config instance to the `StatsActor.props(Config)` method, or use the parameterless
method `StatsActor.props()` to rely on `ConfigFactory.load()` to resolve settings. Here are the default settings,
with the exception of hostname, which is a required setting:

```
deploymentzone {
    akka-actor-statsd {
        hostname = "required"
        port = 8125
        namespace = ""
        # common packet sizes:
        # fast ethernet:        1432
        # gigabit ethernet:     8932
        # commodity internet:    512
        packet-size = 1432
        transmit-interval = 100 ms
    }
}
```

Even if you do not explicitly pass a Config by using one of the other `StatsActor.props(...)` methods downstream actors
in the network still respect the configuration file settings - or the defaults if no configuration file is used.

## Influences

Forked from githup repo at [cfeduke/akka-actor-statsd](https://github.com/cfeduke/akka-actor-statsd)

