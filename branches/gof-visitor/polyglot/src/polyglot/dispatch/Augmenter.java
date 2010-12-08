package polyglot.dispatch;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import polyglot.ast.Node;
import polyglot.types.Context;

public class Augmenter {

    /** EXAMPLE */
    interface Contexter {
	Context context();
    }

    <T extends Node> T addContext(T n, final Context c) {
	return proxy(n, new Contexter() {
	    public Context context() {
		return c;
	    }
	});
    }
    <T extends Node> T addContext2(T n, final Context c) {
	class C extends Mixin<T> implements Contexter {
	    public C(T n) {
		super(n);
	    }

	    public Context context() { return c; }
	}
	return dispatch(new C(n));
    }

    /** END EXAMPLE */

    public static <T> T dispatch(Mixin<T> mixin) {
	ArrayList<Class<?>> interfaces = new ArrayList<Class<?>>();
	for (Object z = mixin; z != null; ) {
	    Class<?> c = mixin.getClass();
	    for (Class<?> s = c; s != null; s = s.getSuperclass()) {
		for (Class<?> ci : s.getInterfaces()) {
		    interfaces.add(ci);
		}
	    }
	    if (z instanceof Mixin) { z = ((Mixin) z).next; }
	}
	Object p = java.lang.reflect.Proxy.newProxyInstance(mixin.getClass().getClassLoader(), interfaces.toArray(new Class[interfaces.size()]), mixin);
	return (T) p;
    }

    class Mixin<T> implements InvocationHandler {
	private T next;

	Mixin(T next) {
	    this.next = next;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    Class c = this.getClass();
	    
	    Class[] argTypes = new Class[args.length];
	    for (int i = 0; i < args.length; i++) {
		argTypes[i] = args[i] != null ? args[i].getClass() : null;
	    }

	    try {
		Method m2 = this.getClass().getDeclaredMethod(method.getName(), method.getParameterTypes());
		return m2.invoke(this, args);
	    }
	    catch (NoSuchMethodException e) {
	    }
	    
	    return invoke(next, method, args);
	}
    }

    public static <T> T proxy(Class<? super T> c, T target, Object... ext) {
	return (T) java.lang.reflect.Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, new AugmentProxy(target, ext));
    }

    public static <T> T proxy(T target, Object... ext) {
	Class<?> c = target.getClass();
	ArrayList<Class<?>> interfaces = new ArrayList<Class<?>>();
	for (Class<?> s = c; s != null; s = s.getSuperclass()) {
	    for (Class<?> ci : s.getInterfaces()) {
		interfaces.add(ci);
	    }
	}
	Object p = java.lang.reflect.Proxy.newProxyInstance(c.getClassLoader(), interfaces.toArray(new Class[interfaces.size()]), new AugmentProxy(target, ext));
	return (T) p;
    }

    static class AugmentProxy<T> implements InvocationHandler {
	T target;
	Object[] ext;

	AugmentProxy(T target, Object... ext) {
	    this.target = target;
	    this.ext = ext;
	}

	public static <T> Object invoke(AugmentProxy<T> proxy, String name, Object[] args) throws java.lang.Throwable {
	    args = Dispatch.DispatchProxy.flatten(args);

	    Class[] argTypes = new Class[args.length];
	    for (int i = 0; i < args.length; i++) {
		argTypes[i] = args[i] != null ? args[i].getClass() : null;
	    }

	    Method m;
	    Object t;

	    try {
		Class<?> c = proxy.target.getClass();
		m = Dispatch.DispatchProxy.findMethod(c, name, null, argTypes);
		t = proxy.target;
		// This finds the method with exactly formal parameter types ==
		// argTypes.
		// m = c.getMethod(name, argTypes);
	    }
	    catch (NoSuchMethodException e) {
		for (Object ext : proxy.ext) {
		    try {
			Class cx = ext.getClass();
			m = Dispatch.DispatchProxy.findMethod(cx, name, null, argTypes);
			t = ext;
			break;
		    }
		    catch (NoSuchMethodException ex) {
		    }
		}
		throw e;
	    }

	    try {
		return m.invoke(t, args);
	    }
	    catch (IllegalArgumentException e) {
		throw e;
	    }
	    catch (IllegalAccessException e) {
		throw e;
	    }
	    catch (InvocationTargetException e) {
		throw e.getTargetException();
	    }
	}

	public Object invoke(Object proxy, java.lang.reflect.Method m, Object[] args) throws java.lang.Throwable {
	    return invoke(this, m.getName(), args);
	}
    }

}
