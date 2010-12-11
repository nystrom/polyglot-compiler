package funicular

object Streams {
    // Filter from S to T
    trait Filter[S,T] extends Pipeline[S,T] { }
    trait Source[S] extends Pipeline[S,S] { }

    trait Pipeline[S,T] {
        def peek: T
        def pop: T
        def push(v: S)

        // (s->t) --> (t->u)
        def -->[U](filter: Pipeline[T,U]): Pipeline[S,U]

        // (s->t) split (s->u)
        def split[U](filter: Pipeline[S,U]): Split[S,T,U]
    }

    trait Split[S,T,U] extends Pipeline[S,Either[T,U]] {
        def split[V](filter: Pipeline[S,V]): Split[S,Either[T,U],V]

        // join:
        // ((s->t) split (s->u)) --> ((t,u)->v)
        def -->[V](filter: Pipeline[Either[T,U],V]): Pipeline[S,V]
    }

    def feedbackLoop[S,T,U,V](incoming: Pipeline[S,T], forward: Pipeline[T,U], back: Pipeline[U,T], outgoing: Pipeline[U,V]): Pipeline[S,V]
        = throw new RuntimeException("unimplemented")
}
