package funicular.array

import funicular._
import funicular.runtime.Runtime

class RichSeq[A](a: Seq[A]) extends Proxy {
    def self = a
    def inParallel = new ParSeq[A](a)
    def async = new ParSeq[A](a)
    def asynchronously = new ParSeq[A](a)
}



