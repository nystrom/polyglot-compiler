package funicular

object Config {
//    lazy val NTHREADS = (java.lang.Runtime.getRuntime.availableProcessors * 3.5).toInt
    lazy val NTHREADS = 35 //java.lang.Runtime.getRuntime.availableProcessors
    val BOUND = 100
    val MAX_WORKERS = 1000
    val MAXTHREADS = 32767
}
