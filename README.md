# akka-actor-statsd

A dead simple [statsd] client written in Scala as an actor using the [akka] framework.

Currently in pre-release stage; its running, just not yet in production.

## Examples

TODO examples

## Installation

Snapshots are hosted on the Sonatype OSS repository:

```scala
/* sbt */
resolvers ++= Seq("Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

libraryDependencies ++= Seq("com.deploymentzone" %% "akka-actor-statsd" % "0.1-SNAPSHOT")
```

## Explanation

This implementation is intended to be very high performance though I haven't yet benchmarked it and as with
any sub-1.0 release YMMV. (Of course it is open source so feel free to contribute!)

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


## Configuration

I have placed `TODO` blocks in the code where future configuration settings will be implemented. As of this version no
configuration other than the log level is possible.

## Influences

- [statsd-scala] Straight forward neat implementation, but no actors means that by default message transmission - up
    until when the UDP packet is handed off to the kernel - will happen on the calling thread.
- [akka-statsd] A trait for extending an actor which is a pretty neat take, except by following the intended
    implementation causes your actors to violate single responsibility principle and transmits stat data on the actor's
    thread.
- [statsd-akka] Found this one recently, was close to what I was looking for, but no batch operations and will also
    occasionally drop UDP messages to the kernel if too many are queued all at once.

[statsd]: https://github.com/etsy/statsd
[akka]: http://akka.io
[OSS Sonatype]: https://oss.sonatype.org/index.html#welcome
[statsd-scala]: https://github.com/benhardy/statsd-scala
[akka-statsd]: https://github.com/themodernlife/akka-statsd
[statsd-akka]: https://github.com/archena/statsd-akka
