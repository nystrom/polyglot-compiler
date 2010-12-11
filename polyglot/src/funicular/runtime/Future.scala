/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package funicular.runtime

import funicular._

/**
 * The representation of a future expression.
 */
class Future[A](name: String, eval: () => A, initClocks: Seq[funicular.Clock]) extends funicular.Future[A] {
  /**
   * Latch for signaling and wait
   */
  private val latch = new Latch
  private var isStarted = false
  private val startLock = new Lock

  import scala.collection.mutable.ListBuffer
  private var clockBuf: ListBuffer[funicular.Clock] = ListBuffer[funicular.Clock]() ++ initClocks.toList
  def clocks = clockBuf.toList

  /**
   * Set if the activity terminated with an exception.
   * Can only be of type Error or RuntimeException
   */
  private var exception: Option[Throwable] = None
  private var result: Option[A] = None

  def forced: Boolean = latch.apply
  def started: Boolean = startLock.withLock { isStarted }

  def force = {
    start
    latch.await
    exception match {
      case Some(e: Error) => throw e
      case Some(e: RuntimeException) => throw e
      case Some(e) => throw new RuntimeException(e)
      case None => ()
    }
    result match {
      case Some(v) => v
      case None => throw new RuntimeException("future forced, but no value")
    }
  }

  def start: Unit = {
    println("starting " + this)
    startLock.withLock {
      if (isStarted) {
        return
      }
      isStarted = true
    }
    println("spawning " + this)
    Runtime.runAsync(clocks, this.run, this.toString)
    println("spawned " + this)
  }

  private def run: Unit = {
    println("running " + this)
    try {
      result = Some(eval())
    } catch {
      case t: Throwable => {
          exception = Some(t)
        }
    }
    finally {
    println("Releasing latch: " + this.toString)
      latch.release
    }
  }

  override def toString = name
}

object Future{
	var i = 0;
	def getI = {i=i+1; i}
}

// vim:shiftwidth=4:tabstop=4:expandtab
