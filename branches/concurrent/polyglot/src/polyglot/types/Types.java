package polyglot.types;

import java.util.ArrayList;
import java.util.List;

import polyglot.util.InternalCompilerError;

public class Types {
    public static <T> T get(Ref<T> ref) {
	return ref != null ? ref.get() : null;
    }
    
    /** Return a reference to v. */
    public static <T> Ref<T> ref(T v) {
	if (v == null)
	    return null;
	Ref_c<T> r = new Ref_c<T>(v);
	return r;
    }
    public static <T> Ref<T> lazyRef() {
        // future and clock not set
	Ref_c<T> r = new Ref_c<T>();
	return r;
    }
    public static <T> Ref<T> lazyRef(T defaultValue) {
        // future and clock not set
        Ref_c<T> r = new Ref_c<T>();
        r.updateSoftly(defaultValue);
	return r;
    }
    public static <T> Ref<T> lazyRef(T defaultValue, final Ref.Callable<T> resolver, funicular.Clock clock) {
	Ref_c<T> r = new Ref_c<T>(resolver, clock);
	r.updateSoftly(defaultValue);
	return r;
    }
    public static <T> Ref<T> lazyRef(T defaultValue, final Runnable resolver, funicular.Clock clock) {
	Ref_c<T> r = new Ref_c<T>();
	r.updateSoftly(defaultValue);
	r.setResolver(resolver, clock);
	return r;
    }
}
