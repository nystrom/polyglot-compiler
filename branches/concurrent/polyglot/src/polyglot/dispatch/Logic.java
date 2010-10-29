package polyglot.dispatch;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import polyglot.frontend.Globals;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class Logic {
    static <T> Promise<T> p(T x) {
	Promise<T> p = new Promise<T>() {
	    T t;
	    boolean bound;
	    
	    public T get() {
		if (! bound)
		    throw new InternalCompilerError("blarg");
		return t;
	    }
	    
	    public void equate(Promise<T> p) {
		if (bound) {
		    assert t.equals(p.get());
		    return;
		}
		t = p.get();
	    }
	};
	// p.equate(x);
	return p;
    }
    static <T> Promise<T> p() {
	return Logic.<T>p(null);
    }
    interface Promise<T> {
	void equate(Promise<T> p);
	T get();
    }
    interface PromiseList<T> extends List<Promise<T>> { }
    interface MonotonePromise<T> {
	void accumulate(MonotonePromise<T> p);
	void accumulate(Promise<T> p);
	T force();
	T join(T s, T t);
    }
    interface Rules {
	Promise<Boolean> isSubtype(Promise<? extends Type> sub, Promise<? extends Type> sup);
	PromiseList<MethodDef> methodLookup(Promise<? extends Type> container, Promise<Name> name, PromiseList<? extends Type> args);
    }
    
    static InvocationHandler handler = new Handler();
    
    static <T> T newInstance(Class<T> c) {
	Object p = java.lang.reflect.Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, handler);
	return (T) p;
    }

    static class Handler implements InvocationHandler {
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    Class c = proxy.getClass();
	    
	    System.out.println("c = " + c);
	    System.out.println("method = " + method);
	    
	    return method.invoke(proxy, args);
	}
    }
    
    public static void main(String[] args) {
	Rules st = Logic.newInstance(Rules.class);
	UnknownType UT = Globals.TS().unknownType(Position.COMPILER_GENERATED);
	st.isSubtype(p(UT), p(UT));
    }
}
