package ibex.runtime;

public interface ITuple<S,T extends ITuple> {
    public Object get(int i);
    public S fst();
    public T snd();
}
