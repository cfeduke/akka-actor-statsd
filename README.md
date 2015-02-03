# akka-actor-statsd

A dead simple [statsd] client written in Scala as an actor using the [akka] framework.

Currently at 0.4 snapshot. We're using this software in a non-production setting
at [TrafficLand](http://www.trafficland.com).

0.4 is compiled with Scala 2.10.4 against Akka 2.3.4 which works with Play 2.3.4.

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
libraryDependencies ++= Seq("com.deploymentzone" %% "akka-actor-statsd" % "0.2")
```

Snapshots are hosted on the Sonatype OSS repository.

```scala
resolvers ++= Seq("Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
```

This library requires [Akka](http://akka.io) 2.3 to get around a bug with 
[Stash and TestActorRef](http://stackoverflow.com/questions/21725473/creating-a-testactorref-results-in-nullpointerexception/22432436#22432436) for test purposes only. It is compatible with Akka 2.2.3. If you need to keep a dependency on Akka 2.2.3 (for use with [scala-redis-nb](https://github.com/debasishg/scala-redis-nb/tree/master) for example) be sure to use an exclusion rule:

```scala
libraryDependencies ++= Seq("com.deploymentzone" %% "akka-actor-statsd" % "0.2"
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

- [statsd-scala] Straight forward neat implementation, but no actors means that by 
    default message transmission - up until when the UDP packet is handed off to the kernel - will happen on the calling thread.
- [akka-statsd] A trait for extending an actor which is a nice take, except by
    following the intended implementation causes your actors to violate single responsibility principle and transmit stat data on the actor's thread.
- [statsd-akka] Found this after I began my version, very close to what I had 
    originally envisioned.

[statsd]: https://github.com/etsy/statsd
[akka]: http://akka.io
[OSS Sonatype]: https://oss.sonatype.org/index.html#welcome
[statsd-scala]: https://github.com/benhardy/statsd-scala
[akka-statsd]: https://github.com/themodernlife/akka-statsd
[statsd-akka]: https://github.com/archena/statsd-akka
