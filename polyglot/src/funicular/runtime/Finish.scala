/*
 *
 * (C) Copyright IBM Corporation 2006-2008
 *
 *  This file is part of X10 Language.
 *
 */

package funicular.runtime

import funicular.MultipleExceptions

/**
 * @author tardieu
 */
class Finish {
    private val lock = new Lock
    private var exceptions: List[Throwable] = Nil
    private var activities: List[Activity] = Nil

    def join: Unit = {
        lock.withLock {
            while (true) {
                activities match {
                    case Nil => return
                    case a::as => {
                        activities = as
                        lock.withoutLock {
                            a.join
                        }
                    }
                }
            }
        }
    }

    def joinAndThrow = {
        join
        throwExceptions
    }

/*
    def runTop(a: Activity) = {
        lock.withLock {
            activities = a::activities
        }
        Runtime.pool.invoke(a)
    }
    */

    def run(a: Activity) = {
        lock.withLock {
            activities = a::activities
        }
        a.fork
        //Runtime.pool.execute(a)
    }

    def runAsync[A](clocks: Seq[funicular.Clock], body: => A, id:String): Unit = {
      
      val a = new Activity(() => body, this, clocks)
      println("Finish.runAsyncClocked: Created new activity for - " + id + " with objectId " + a)
      run(a)
    }

    def runAsync[A](body: => A): Unit = {
      run(new Activity(() => body, this))
    }

    def throwExceptions = {
        lock.withLock {
            exceptions match {
                case Nil => null
                case e::Nil => throw e
                case es => throw new MultipleExceptions(es)
            }
        }
    }

    def pushException(t:Throwable) = lock.withLock {
        exceptions = t::exceptions
    }
}

// vim:shiftwidth=4:tabstop=4:expandtab
