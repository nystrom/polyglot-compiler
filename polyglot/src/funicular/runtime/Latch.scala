/*
 *
 * (C) Copyright IBM Corporation 2006-2008.
 *
 *  This file is part of X10 Language.
 *
 */

package funicular.runtime

/**
 * Boolean latch
 * @author tardieu
 */
class Latch extends Monitor with Function0[Boolean] {
    private var state: Boolean = false

    override def release = {
        lock
        state = true
        super.release
    }

    override def await = {
        // avoid locking if state == true
        if (!state) {
            withLock {
                while (!state) {
                    super.await
                }
            }
        }
    }

    def apply = state // memory model?
}

// vim:shiftwidth=4:tabstop=4:expandtab
