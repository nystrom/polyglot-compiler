package ibex.runtime;

public class Tuple<S, T extends ITuple> implements ITuple<S, T> {
    Object[] data;
    int offset;

    public static <S, T extends ITuple> ITuple<S, T> make(Object... a) {
        return new Tuple(a);
    }
    
    private Tuple(Object[] a) {
        data = a;
        offset = 0;
    }
    private Tuple(int o, Object[] a) {
        data = a;
        offset = o;
    }
    
    public Object get(int i) {
        return data[i];
    }
    
    public S fst() {
        return (S) data[0];
    }
    
    public T snd() {
        return (T) new Tuple(offset+1, data);
    }
}
