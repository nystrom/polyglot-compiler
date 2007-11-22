package polyglot.types;

import polyglot.frontend.Goal;

public class Types {

    public static <T> T get(Ref<T> ref) {
        return ref != null ? ref.get() : null;
    }

    public static <T extends TypeObject> Ref_c<T> ref(T v) {
        return v != null ? new Ref_c<T>(v) : null;
    }

    public static <T extends Def> Symbol<T> symbol(T v) {
        Symbol<T> sym = new Symbol_c<T>(v);
        v.setSymbol(sym);
        return sym;
    }
    
    public static <T extends TypeObject> LazyRef<T> typeRef() {
        return new LazyRef_c<T>();
    }

    /** Create a lazy reference to a type object, with an initial value.
     * @param v initial value
     * @param resolver goal used to bring the reference up-to-date
     * 
     * ### resolver should be a map
     */
    public static <T extends TypeObject> LazyRef<T> typeRef(T v, Goal resolver) {
        return new LazyRef_c<T>(v, resolver);
    }

}
