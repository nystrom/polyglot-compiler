package funicular.array

import funicular.{foreach => par, _}
import funicular.Future
import funicular.runtime.Runtime

class ParArray[A: ClassManifest](a: Array[A], P: Int) extends Proxy {
    def self = a

    // Numeric operations
    def sum(implicit num: Numeric[A]) = parArray(a).reduce(num.zero)(num.plus)
    def product(implicit num: Numeric[A]) = parArray(a).reduce(num.one)(num.times)
    def max(implicit num: Numeric[A]) = parArray(a).reduce(num.max _)
    def min(implicit num: Numeric[A]) = parArray(a).reduce(num.min _)
    def +(b: Array[A])(implicit num: Numeric[A]) = Array.ofDim[A](a.length).parInit(i => num.plus( a(i) , b(i)))
    def -(b: Array[A])(implicit num: Numeric[A]) = Array.ofDim[A](a.length).parInit(i => num.minus( a(i) , b(i)))
    def *(b: Array[A])(implicit num: Numeric[A]) = Array.ofDim[A](a.length).parInit(i => num.times( a(i) , b(i)))
    def max(b: Array[A])(implicit num: Numeric[A]) = Array.ofDim[A](a.length).parInit(i => num.max(a(i), b(i)))
    def min(b: Array[A])(implicit num: Numeric[A]) = Array.ofDim[A](a.length).parInit(i => num.min(a(i), b(i)))
    def negate(implicit num: Numeric[A]) = Array.ofDim[A](a.length).parInit(i => num.negate(a(i)))
    def abs(implicit num: Numeric[A]) = Array.ofDim[A](a.length).parInit(i => num.abs(a(i)))

    def flatMap[B](f: A => Iterable[B]): Seq[B] = {
        val spawn = (0 until a.length).map(j => future { f(a(j)) })
        new ParSeq[B](spawn.map((fi: Future[Iterable[B]]) => fi.force).flatMap(identity))
/*

        val result = Array.ofDim[Iterable[Future[Iterable[B]]]](P)
        for (i <- 0 until P) {
            val scale = (a.length + P - 1) / P
            val min = i*scale
            val max = math.min((i+1)*scale, a.length)
            val s = (min until max).map(a).map(aj => future { f(aj) })
            result(i) = s
        }
        new ParSeq[B](result.flatMap(identity).map(_.force).flatMap(identity))
        */
    }

    def filter(p: A => Boolean): Seq[A] = {
        val spawn = Array.ofDim[Future[Seq[A]]](P)
        for (i <- 0 until P) {
            val scale = (a.length + P - 1) / P
            val min = i*scale
            val max = math.min((i+1)*scale, a.length)
            spawn(i) = future[Seq[A]] {
                for (j <- min until max; if p(a(j)))
                        yield a(j)
            }
        }
        val seq = (0 until spawn.length).map(spawn)
        new ParSeq[A](seq.map(_.force).flatMap(identity))
    }

    def map[B](f: A => B): Seq[B] = {
        val spawn = Array.ofDim[Future[Seq[B]]](P)
        for (i <- 0 until P) {
            val scale = (a.length + P - 1) / P
            val min = i*scale
            val max = math.min((i+1)*scale, a.length)
            spawn(i) = future[Seq[B]] {
                for (j <- min until max) yield f(a(j))
            }
        }
        val seq = (0 until spawn.length).map(spawn)
        new ParSeq[B](seq.map(_.force).flatMap(identity))
    }

    def foreach[B](f: A => B): Unit = {
        // TODO: 
        // break in two
        // recursively break the first half in two
        // repeat until 8x NPROCS
        // so, next block is twice as large as current block
        // 112-4---8-------16--------------
        // --
        // also break recursively in two when stealing
        // don't break recursively in two if not stealing
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

    def copy = wrapArray(a).parInit((i:Int) => a(i))

    /**
     * Map the array to another array, in parallel.
     */
    def mapPar[B: ClassManifest](f: A => B): Array[B] = lift[B](f)
    def lift[B: ClassManifest](f: A => B): Array[B] = 
        Array.ofDim[B](a.length).parInit(i => f(a(i)))

    /**
     * Reduce the array with function f.  f must be associative.
     * Throws UnsupportedOperationException if the array is empty.
     */
    def reduce(f: (A,A) => A): A = {
        if (a.length == 0)
            throw new UnsupportedOperationException
        
        val r = Array.ofDim[A](P)

        finish {
            par (0 until P) {
                i => {
                    val scale = (a.length + P - 1) / P
                    val min = i*scale
                    val max = math.min((i+1)*scale, a.length)
                    var x = a(min)
                    for (j <- min+1 until max)
                        x = f(x, a(j))
                    r(i) = x
                }
            }
        }

        var x = r(0)
        for (i <- 1 until P)
            x = f(x, r(i))
        x
    }

    def reduce(z: A)(f: (A,A) => A): A = {
        val r = Array.ofDim[A](P)

        finish {
            par (0 until P) {
                i => {
                    val scale = (a.length + P - 1) / P
                    val min = i*scale
                    val max = math.min((i+1)*scale, a.length)
                    var x = z
                    for (j <- min until max)
                        x = f(x, a(j))
                    r(i) = x
                }
            }
        }

        var x = z
        for (i <- 0 until P)
            x = f(x, r(i))
        x
    }
}
