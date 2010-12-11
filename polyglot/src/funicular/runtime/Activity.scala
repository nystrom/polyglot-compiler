/*
*
* (C) Copyright IBM Corporation 2006-2008.
*
*  This file is part of X10 Language.
*
*/

package funicular.runtime

/**
 * @author tardieu
 */
class Activity(body: () => _, val finish: Finish, val clocks: Seq[funicular.Clock]) extends jsr166y.RecursiveAction {
  /**
   * Create clocked activity.
   */
  def this(body: () => _, finish: Finish) {
    this(body, finish, null)
  }
  
  registerClocks

  def registerClocks = {
    if (null != clocks)
      for (clock <- clocks) {
        clock match {
          case rtclock: funicular.runtime.Clock => rtclock.register(this)
          case _ => throw new funicular.ClockUseException
        }
      }
  }

  def next: Unit = {
    if (clocks != null)
      for (clock <- clocks)
        clock.next
  }

  def dropClocks: Unit = {
    if (clocks != null)
      for (clock <- clocks)
        clock.drop
  }

  /**
   * the finish state governing the execution of this activity
   */
  var innermostFinish: Finish = finish

  def runFinish[B](body: => B): Unit = {
    val old = innermostFinish
    val f = new Finish
    try {
      innermostFinish = f
      body
      f.joinAndThrow
    } finally {
      innermostFinish = old
    }
  }

  override def compute: Unit = {
    val old = Runtime.myActivity.get
    try {
      Runtime.myActivity.set(this)
      body()
    } catch {
      case t: Throwable => innermostFinish.pushException(t)
    }
    finally {
      if (null != clocks)
        for (clock <- clocks)
          clock.drop
      Runtime.myActivity.set(old)
    }
  }
}

// vim:shiftwidth=4:tabstop=4:expandtab
