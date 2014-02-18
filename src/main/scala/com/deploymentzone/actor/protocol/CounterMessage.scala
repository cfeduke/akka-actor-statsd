package com.deploymentzone.actor.protocol

abstract class CounterMessage[T](val bucket: String, val value: T, val samplingRate: Double = 1.0) {
  val symbol: String

  override def toString =
    samplingRate match {
      case 1.0  => s"$bucket:$value|$symbol"
      case _    => ""
    }
}
