/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package funicular.runtime

import scala.collection.mutable.Stack


/**
 * Lock with wait/notify capability implemented using park/unpark
 * @author tardieu
 */
class Monitor extends Lock {
    /**
     * Parked threads
     */
    private val threads = new Stack[Thread]

    /**
     * Park calling thread
     * Increment blocked thread count
     * Must be called while holding the lock
     * Must not be called while holding the lock more than once
     */
    def await = {
        val thread = Runtime.currentThread
        threads.push(thread)
        while (threads.contains(thread)) {
            withoutLock {
                Runtime.park
            }
        }
    }

    /**
     * Unpark every thread
     * Decrement blocked thread count
     * Release the lock
     * Must be called while holding the lock
     */
    def release = {
        val size = threads.size
        if (size > 0) {
            for (i <- 0 until size)
            	Runtime.unpark(threads.pop)
        }
        unlock
    }
}

// vim:shiftwidth=4:tabstop=4:expandtab
