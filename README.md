# akka-actor-statsd

A dead simple [statsd] client written in Scala as an Akka actor.

## Influences

- [statsd-scala] Straight forward neat implementation, but no actors means that by default message transmission - up until when the UDP packet is handed off to the kernel - will happen on the calling thread.
- [akka-statsd] A trait for extending an actor which is a pretty neat take, except it violates single responsibility principle and transmits stat data on the actor's thread.

[statsd]: https://github.com/etsy/statsd
[statsd-scala]: https://github.com/benhardy/statsd-scala
[akka-statsd]: https://github.com/themodernlife/akka-statsd
