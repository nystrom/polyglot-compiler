/*
*
* (C) Copyright IBM Corporation 2006-2008.
*
*  This file is part of X10 Language.
*
*/

package funicular.runtime

import scala.collection.mutable.HashMap
import scala.util.Random
import scala.collection.mutable.Stack
import funicular._

import java.util.concurrent.atomic.AtomicBoolean

/**
* @author tardieu
*/
object Runtime {
    val pool = new jsr166y.ForkJoinPool(concurrency)

    lazy val concurrency = funicular.Config.NTHREADS

    private val monitor = new Monitor

    def currentThread = Thread.currentThread

    /**
    * Return the current activity
    */
    def activity = myActivity.get
    val myActivity = new ThreadLocal[Activity](null)

    def finish = {
        val a = activity
        if (a != null)
            a.innermostFinish
        else
            null
    }

    /**
     * Run main activity in a finish
     */
    def runFinish(body: => Unit): Unit = {
        val a = activity
        if (a != null) {
            a.runFinish(body)
        }
        else {
            val f = new Finish
            val a = new Activity(() => body, f)
            Runtime.pool.invoke(a)
            f.joinAndThrow
        }
    }

    /**
     * Run async
     */
    def runAsync[A](clocks: Seq[funicular.Clock], body: => A): Unit = {
        val f = finish
        if (f == null)
            throw new RuntimeException("Cannot run an async outside a finish.")
        f.runAsync(clocks, body)
    }

    def runAsync[A](body: => A): Unit = {
        val f = finish
        if (f == null)
            throw new RuntimeException("Cannot run an async outside a finish.")
        f.runAsync(body)
    }

    /**
     * Eval future expression
     */
    def evalFuture[A](eval: () => A, clocks: Seq[funicular.Clock]): Future[A] = {
        val f1 = evalDelayedFuture[A](eval, clocks)
        f1.start
        f1
    }

    /**
     * Eval delayed future expression
     */
    def evalDelayedFuture[A](eval: () => A, clocks: Seq[funicular.Clock]): Future[A] = {
        new Future[A]("future"+Future.getI, eval, clocks)
    }

    /**
    * Lock current place
    * not reentrant!
    */
    def lock = monitor.lock

    /**
    * Wait on current place lock
    * Must be called while holding the place lock
    */
    def await = monitor.await

    /**
    * Unlock current place
    * Notify all
    */
    def release = monitor.release

    // sleep

    /**
     * Sleep for the specified number of milliseconds.
     * [IP] NOTE: Unlike Java, funicular sleep() simply exits when interrupted.
     * @param millis the number of milliseconds to sleep
     * @return true if completed normally, false if interrupted
     */
    def sleep(millis: Long):Boolean = {
        val a = activity
        assert(false, "sleep is broken")
        try {
            java.lang.Thread.sleep(millis)
            return true
        }
        catch {
            case e:InterruptedException => {
                return false
            }
        }
    }

    // clocks

    /**
     * Next statement = next on all clocks in parallel.
     */
    def next = activity.next

    // park current thread
    def park = java.util.concurrent.locks.LockSupport.park

    def parkNanos(nanos: Long) = java.util.concurrent.locks.LockSupport.parkNanos(nanos)

    // unpark given thread
    def unpark(thread: Thread) = java.util.concurrent.locks.LockSupport.unpark(thread)
}

// vim:shiftwidth=4:tabstop=4:expandtab
