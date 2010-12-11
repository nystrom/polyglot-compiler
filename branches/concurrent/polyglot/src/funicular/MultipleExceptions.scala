package funicular

class MultipleExceptions(val exceptions: Collection[Throwable]) extends Exception {
    override def printStackTrace = {
        super.printStackTrace
        var msg = "Caused by:"
        for (th <- exceptions) {
            println(msg)
            th.printStackTrace
            msg = "and by:"
        }
    }
}

object MultipleExceptions {
    def unapply(v: Any) = {
        if (v.isInstanceOf[MultipleExceptions])
            Some(v.asInstanceOf[MultipleExceptions].exceptions.toList)
        else if (v.isInstanceOf[Exception])
            Some(v.asInstanceOf[Exception]::Nil)
        else
            None
    }
}
