/**
 * 
 */
package polyglot.util;


public interface Option<T> {
    T get();

    public static class Some<T> implements Option<T> {
        T t;

        public Some(T t) {
            this.t = t;
        }

        public T get() {
            return t;
        }
    }

    public static class None<T> implements Option<T> {
        public None() {}

        public T get() {
            return null;
        }
    }
}