# akka-actor-statsd

A dead simple [statsd] client written in Scala as group of actors using the [akka] framework.

## Naming conventions

For The New Motion applications use the following naming conventions:

A 'bucket' consists of a namespace part and a hierarchy part

the namespace is set by adding `[environment].[application]` in the application.conf

environment can be `test`, `sandbox`, `prod` and even `unittest`
the application a one or two word description of the application separated by dashes

Think about the hierarchy you want to use inside your application, this will be of great influence on the representation side

## Examples

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

### API

There is a simple api if you dont want to use the actor's `!` method's

```scala
implicit val statsActor = context.actorOf(Stats.props())

Stats.increment(Bucket("endpoint1"))
Stats.timing(Bucket("page2.response"))(250 milliseconds)

Stats.withTimer(Bucket("code.execution.time")) {
    //execute code here
    Thread.sleep(new Random().nextInt())
}
```



## Installation

Releases and snapshots are hosted on The New Motion public repository. To add dependency to your project use following snippet:

```scala
resolvers += "The New Motion Public Repo" at "http://nexus.thenewmotion.com/content/groups/public/"

libraryDependencies += "com.thenewmotion" %% "akka-statsd-core" % "0.9.0"
```

For stats collection over HTTP requests served by spray server or issued by spray client, use respective dependencies from below:
```
libraryDependencies += "com.thenewmotion" %% "akka-statsd-spray-server" % "0.9.0"
libraryDependencies += "com.thenewmotion" %% "akka-statsd-spray-client" % "0.9.0"
```


## Explanation

This implementation is intended to be of high performance, thus it

- uses an akka.io UdpConnected implementation to cache local security checks within the JVM if SecurityManager is enabled
- allows for message batching before sending them out to StatsD, as per [statsd Multi-Metric Packets](https://github.com/etsy/statsd/blob/master/docs/metric_types.md#multi-metric-packets) specification. Turned on by default

Supports all of the [StatsD Metric Types](https://github.com/etsy/statsd/blob/master/docs/metric_types.md) including optional sampling parameters.

| StatsD               | akka-actor-statsd       |
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

"""
akka.statsd.transformations = [
  {
    pattern = "/foo/[a-z0-9]+/bar",
    into    = "/foo/[segment]/bar"
  }
]
"""

This reads as: if the part of the bucket matches the regular expression in `pattern`, replace it with the one described in `into` (which is *not* a regular expression).

Please note that:
- client-defined transformations take priority over `UUID` replacement
- transformations are matched from top to bottom
- every transformation that matches the path will be applied


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

## Compatibility

Since version `0.9.0`, if typesafe configuration used, provide settings in namespace `akka.statsd`.

Versions before `0.9.0` use `deploymentzone.statsd`

## Influences

Forked from githup repo at [cfeduke/akka-actor-statsd](https://github.com/cfeduke/akka-actor-statsd)

