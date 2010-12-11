package funicular.array

import funicular.{foreach => par, finish, future}
import funicular.runtime.Runtime

class ParSeq[A](a: Seq[A]) extends Proxy with Seq[A] {
    private lazy val P = Runtime.concurrency

    def self = a

    override def filter(p: A => Boolean): Seq[A] = a.filter(p)
    def apply(i: Int): A = a.apply(i)
    def length = a.length
    def iterator = a.iterator

    override def foreach[B](f: A => B): Unit = {
        par (0 until P) {
            i => {
                val scale = (a.length + P - 1) / P
                val min = i*scale
                val max = math.min((i+1)*scale, a.length)
                for (j <- min until max)
                    f(a(j))
            }
        }
    }
}
