package funicular.runtime

import java.lang.{ThreadLocal => TL}

class ThreadLocal[A](init: => A) extends TL[A] with Function0[A] {
  override def initialValue: A = init
  def apply = get
  def withValue[B](thunk: A => B) = thunk(get)
}

