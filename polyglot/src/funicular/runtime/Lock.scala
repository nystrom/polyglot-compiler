/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package funicular.runtime

import java.util.concurrent.locks.ReentrantLock


/**
 * Lock with wait/notify capability implemented using park/unpark
 * @author tardieu
 */
class Lock extends ReentrantLock {
    def withLock[T](body: => T) =
        try {
            lock
            body
        }
        finally {
            unlock
        }

    def withoutLock[T](body: => T) =
        try {
            unlock
            body
        }
        finally {
            lock
        }
}
