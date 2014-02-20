package com.deploymentzone.actor.domain

import scala.collection.immutable.Queue
import com.deploymentzone.actor.PacketSize
import scala.annotation.tailrec
import java.nio.charset.Charset

/**
 * Logic for combining messages so they won't cross a predetermined packet size boundary.
 *
 * This class is not thread-safe.
 *
 * @param packetSize maximum byte size that is permitted for any given payload
 */
private[actor] class MultiMetricQueue(val packetSize: Int) {
  var queue = Queue[String]()

  def enqueue(elem: String) = {
    queue = queue.enqueue(elem)
    this
  }

  def payload(): String = {
    @tailrec
    def recurse(acc: StringBuilder = new StringBuilder, utf8Length: Int = 0): String = {
      val utf8 = Charset.forName("utf-8")
      queue.isEmpty match {
        case true => acc.toString()
        case false =>
          val proposedAddition = queue.head.getBytes(utf8).length
          if (proposedAddition + utf8Length + 1 > (packetSize + 1))
            acc.toString()
          else {
            val (item, sq) = queue.dequeue
            queue = sq
            acc.append(item)
            acc.append("\n")
            recurse(acc, proposedAddition + utf8Length)
          }
      }
    }
    recurse().stripLineEnd
  }
}

object MultiMetricQueue {
  def apply(packetSize: Int = PacketSize.GIGABIT_ETHERNET) = new MultiMetricQueue(packetSize)
}
