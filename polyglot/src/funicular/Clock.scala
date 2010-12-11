package funicular

import funicular._

object Clock {
    def apply(name:String): Clock = new funicular.runtime.Clock(name)
    def apply(): Clock = new funicular.runtime.Clock("clock")
}

trait Clock {
    def resume: Unit
    def next: Unit
    
    def drop: Unit
    def dropped: Boolean

    def register: Unit
    def registered: Boolean
    
    //def nextT(id : String): Unit
}
