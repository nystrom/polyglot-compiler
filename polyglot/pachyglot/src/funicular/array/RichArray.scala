package funicular.array

import funicular._
import funicular.runtime.Runtime

class RichArray[A: ClassManifest](a: Array[A]) extends Proxy {
    private def P = Runtime.concurrency

    def self = a

    // def get(p: Product2[Int,Int]) = apply(p._1)(p._2)
    // def get(p: Product) = apply(p.get(0).asInstanceOf[Int])(p.get(1).asInstanceOf[Int])

    def inParallel = async
    def asynchronously = async
    def async = new ParArray[A](a, P)

    def unchunked = new ParArray[A](a, a.length)

    def printPar =
        for (ai <- a.inParallel) {
            println(ai)
        }

    def parInit(f: Int => A): Array[A] = {
        finish {
            foreach (0 until a.length) {
                j => {
                  a(j) = f(j)
                }
            }
        }
        a
    }
}
