package akka.statsd.transport

import scala.collection.immutable
import scala.annotation.tailrec
import java.nio.charset.Charset
import akka.event.Logging
import akka.actor.ActorSystem

/**
 * Combines messages into packets of no greater size then specified
 *
 * Takes UTF-8 byte size into account for messages.
 *
 * When a single message is larger than the provided [[packetSize]] that message is dropped
 * with a log warning.
 *
 * This class is not thread-safe.
 *
 * @param packetSize maximum byte size that is permitted for any given payload
 */
private[statsd] class MultiMetricQueue(val packetSize: Int)(implicit system: ActorSystem) {
  val logger = Logging(system.eventStream, classOf[MultiMetricQueue])

  private var queue = immutable.Queue[String]()

  /**
   * Enqueues a message for future dispatch.
   * @param message message to enqueue
   * @return this instance (for convenience chaining)
   */
  def enqueue(message: String) = {
    queue = queue.enqueue(message)
    this
  }

  /**
   * The remaining number of messages in the queue.
   *
   * Primarily provided for instrumentation and testing.
   *
   * @return number of messages remaining in the queue.
   */
  def size: Int = queue.size
  /**
   * Creates a StatsD payload message from a list of messages up to the [[packetSize]] limit
   * in bytes taking UTF-8 size into account.
   *
   * Items that are added to the payload are also removed from the queue.
   *
   * @return Newline separated list of StatsD messages up to the maximum [[packetSize]]
   */
  def payload(): Option[String] = {
    @tailrec
    def recurse(acc: StringBuilder = new StringBuilder, utf8Length: Int = 0): String = {
      val UTF8 = Charset.forName("utf-8")

      queue.isEmpty match {
        case true => acc.toString()
        case false =>
          queue.head.getBytes(UTF8).length match {
            case proposedAddition if proposedAddition > packetSize =>
              DroppedMessageWarning(proposedAddition, queue.head)
              val (_, sq) = queue.dequeue
              queue = sq
              recurse(acc, utf8Length)
            case proposedAddition if proposedAddition + utf8Length + 1 > (packetSize + 1) =>
              acc.toString()
            case proposedAddition =>
              val (item, sq) = queue.dequeue
              queue = sq
              acc.append(item)
              acc.append("\n")
              recurse(acc, proposedAddition + utf8Length)
          }
      }
    }

    recurse().stripLineEnd match {
      case result if result.length > 0 => Some(result)
      case _ => None
    }
  }

  /**
    * Creates as many payloads as necessary to empty the queue.
    * This ensures we flush the whole queue at every flush command and prevents memory growing out of control.
    */
  def flushQueue(): Stream[String] = Stream.continually(payload()).takeWhile(_.nonEmpty).flatten

  private object DroppedMessageWarning extends ((Int, String) => Unit) {
    def apply(proposedAddition: Int, message: String): Unit = {
      if (!logger.isWarningEnabled)
        return

      val DISCARD_MSG_MAX_LENGTH = 25
      val discardMsgLength = message.length
      val ellipsis = discardMsgLength match {
        case n if n > DISCARD_MSG_MAX_LENGTH => "..."
        case _ => ""
      }
      val discardMsg = message.substring(0, Math.min(DISCARD_MSG_MAX_LENGTH, discardMsgLength)) + ellipsis
      logger.warning(s"""Message "$discardMsg" discarded because its size ($proposedAddition) was larger than the permitted maximum packet size ($packetSize)""")
    }
  }
}

object MultiMetricQueue {
  /**
   * Creates an instance of MultiMetricQueue.
   *
   * @param packetSize maximum packet size for a single aggregated message
   */
  def apply(packetSize: Int)(implicit system: ActorSystem) =
    new MultiMetricQueue(packetSize)(system)
}
