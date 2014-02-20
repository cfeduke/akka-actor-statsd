package com.deploymentzone.actor.domain

import scala.collection.immutable.Queue
import com.deploymentzone.actor.PacketSize
import scala.annotation.tailrec

class MultiMetricQueue(val packetSize: Int) {
  var queue = Queue[String]()

  def enqueue(elem: String) = {
    queue = queue.enqueue(elem)
    this
  }

  def payload(): String = {
    @tailrec
    def recurse(acc: StringBuilder = new StringBuilder): String = {
      queue.isEmpty match {
        case true => acc.toString()
        case false =>
          if (queue.head.length + acc.length + 1 >= packetSize)
            acc.toString()
          else {
            val (item, sq) = queue.dequeue
            queue = sq
            acc.append(item)
            acc.append("\n")
            recurse(acc)
          }


      }
    }
    recurse().stripLineEnd
  }
}

object MultiMetricQueue {
  def apply(packetSize: Int = PacketSize.GIGABIT_ETHERNET) = new MultiMetricQueue(packetSize)
}
