package funicular

object Exceptions {
    def unapply(v: Any) = {
        if (v.isInstanceOf[MultipleExceptions])
            Some(v.asInstanceOf[MultipleExceptions].exceptions.toList)
        else if (v.isInstanceOf[Exception])
            Some(v.asInstanceOf[Exception]::Nil)
        else
            None
    }
}
