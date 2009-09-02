package polyglot.dispatch;

import java.lang.reflect.*;
import java.util.*;

public class CopyOfDispatch {
    static
    class Add extends Node {}
    static
    class Sub extends Node {}

    static int add = 0;
    static int sub = 0;
    static int node = 0;
    
    static
    class Visitor2 extends Visitor1 {
	public void visit(Add a) {
	    add++;
//	    System.out.println("add");
	}

	public void visit(Sub s) {
	    sub++;
//	    System.out.println("sub");
	}
    }

    public static void main(String[] args) {
	// Java5 proxy dispatch is 1000x regular dispatch.  Java6 proxy dispatch is 300x.
	// I implemented a (broken) cache, which helped by 2x.
	for (int i = 0; i < 3; i++) {
	    test1();
	    try {
		Thread.sleep(500);
	    }
	    catch (InterruptedException e) {
	    }
	}
	for (int i = 0; i < 3; i++) {
	    test2();
	    try {
		Thread.sleep(500);
	    }
	    catch (InterruptedException e) {
	    }
	}
//	test3();
    }

//    private static void test3() {
//	VoidVisitor2 v2 = new VoidVisitor2();
//	new Sub().accept(v2, "this", "is", 1, "more", "Arg");
//    }

    private static void test2() {
	add = sub = node = 0;
	Node[] b = new Node[] { new Add(), new Sub() };
	long t = System.currentTimeMillis();
	for (int i = 0; i < 1000000; i++) {
	    Node a = b[i%2];
	    Visitor v = new Visitor2();
	    if (a instanceof Sub) {
		((Visitor2) v).visit((Sub) a);
	    }
	    else if (a instanceof Add) {
		((Visitor2) v).visit((Add) a);
	    }
	    else {
		((Visitor2) v).visit((Node) a);
	    }
	}
	System.out.println("t = " + (System.currentTimeMillis() - t));
	assert add == 1000000/2;
	assert sub == 1000000/2;
    }

    private static void test1() {
	add = sub = node = 0;
	Node[] b = new Node[] { new Add(), new Sub() };
	long t = System.currentTimeMillis();
	{
	    Visitor v = new Visitor2();
	    for (int i = 0; i < 1000000; i++) {
		Node a = b[i%2];
		a.accept(v);
	    }
	}
	System.out.println("t = " + (System.currentTimeMillis() - t));
	assert add == 1000000/2;
	assert sub == 1000000/2;
    }

    /**
     * Create a dynamic multiple dispatch proxy object. T must be an interface
     * type.
     */
    public static <T> T proxy(Class<? super T> c, T target) {
	return (T) java.lang.reflect.Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, new DispatchProxy(target));
    }

    static WeakHashMap<Object, Object> cache = new WeakHashMap<Object, Object>();

    public static <T> T proxy(T target) {
	Object p =
	    null; 
	// cache.get(target);
	if (p == null) {
	    Class<?> c = target.getClass();
	    ArrayList<Class<?>> interfaces = new ArrayList<Class<?>>();
	    for (Class<?> s = c; s != null; s = s.getSuperclass()) {
		for (Class<?> ci : s.getInterfaces()) {
		    interfaces.add(ci);
		}
	    }
	    p = java.lang.reflect.Proxy.newProxyInstance(c.getClassLoader(), interfaces.toArray(new Class[0]), new DispatchProxy(target));
//	    cache.put(p, target);
	}
	return (T) p;
    }

    static class DispatchProxy<T> implements InvocationHandler {
	T target;
	
	WeakHashMap<ProxyCacheKey, Method> cache;

	static class ProxyCacheKey {
	    String name;
	    Class[] argTypes;
	    
	    ProxyCacheKey(String name, Class[] argTypes) {
		this.name = name;
		this.argTypes = argTypes;
	    }
	    
	    @Override
	    public boolean equals(Object o) {
		if (o == this)
		    return true;
		if (o instanceof ProxyCacheKey) {
		    ProxyCacheKey k = (ProxyCacheKey) o;
		    if (name.equals(k.name) && argTypes.length == k.argTypes.length) {
			for (int i = 0; i < argTypes.length; i++) {
			    Class c = argTypes[i];
			    Class d = k.argTypes[i];
			    if (! c.equals(d))
				return false;
			}
			return true;
		    }
		}
		return false;
	    }
	    
	    @Override
	    public int hashCode() {
		int h =  name.hashCode();
		for (Class c : argTypes) {
		    h += c.hashCode();
		}
		return h;
	    }
	}

	DispatchProxy(T target) {
	    this.target = target;
	    this.cache = new WeakHashMap<ProxyCacheKey, Method>();
	}

	private boolean compatible(Class[] actuals, Class[] formals) {
	    if (actuals.length != formals.length)
		return false;
	    for (int i = 0; i < actuals.length; i++) {
		if (! formals[i].isAssignableFrom(actuals[i])) 
		    return false;
	    }
	    return true;
	}
	
	private void findMethods(Class<?> c, String name, Class retType, Class[] argTypes, List<Method> matched) throws NoSuchMethodException {
	    if (c == null)
		return;

	    Method[] methods = c.getMethods();

	    for (int i = 0; i < methods.length; i++) {
		Method m = methods[i];
		if (name.equals(m.getName())) {
		    if (compatible(argTypes, m.getParameterTypes()) && (retType == null || retType.isAssignableFrom(m.getReturnType()))) {
			matched.add(m);
		    }
		}
	    }

	    // not needed, getMethods returns them all
	    //	    findMethods(c.getSuperclass(), name, retType, argTypes, matched);
	}

	enum Result {
	    LESS, GREATER, EQUAL, UNKNOWN, INCOMPARABLE;
	}

	Result compareMethods(Method m1, Method m2) {
	    if (m1 == m2)
		return Result.EQUAL;

	    Result r;
	    if (0 < m1.getParameterTypes().length)
	    { 
		{
		    int i = 0;
		    Class t1 = m1.getParameterTypes()[i];
		    Class t2 = m2.getParameterTypes()[i];
		    if (t1.equals(t2))
			r = Result.EQUAL;
		    else if (t2.isAssignableFrom(t1))
			r = Result.LESS;
		    else if (t1.isAssignableFrom(t2))
			r = Result.GREATER;
		    else
			r = Result.INCOMPARABLE;
		}

		for (int i = 1; i < m1.getParameterTypes().length && r != Result.INCOMPARABLE; i++) {
		    Class t1 = m1.getParameterTypes()[i];
		    Class t2 = m2.getParameterTypes()[i];
		    if (t1.equals(t2))
			; // no change
		    else if (t2.isAssignableFrom(t1))
			r = r == Result.GREATER ? Result.INCOMPARABLE : Result.LESS;
		    else if (t1.isAssignableFrom(t2))
			r = r == Result.LESS ? Result.INCOMPARABLE : Result.GREATER;
		    else
			r = Result.INCOMPARABLE;
		}

		if (r == Result.INCOMPARABLE) {
		    // Subclass wins
		    if (m1.getDeclaringClass().isAssignableFrom(m2.getDeclaringClass())) {
			r = Result.LESS;
		    }
		    if (m2.getDeclaringClass().isAssignableFrom(m1.getDeclaringClass())) {
			r = Result.GREATER;
		    }
		}

		return r;
	    }
	    return Result.EQUAL;
	}

	private Method findMethod(Class<?> c, String name, Class<?> retType, Class[] argTypes) throws NoSuchMethodException {
	    if (! useless_search) {
	    Method m = c.getMethod(name, argTypes);
	    return m;
	    }

	    List<Method> matched = new ArrayList<Method>(1);
	    findMethods(c, name, retType, argTypes, matched);
	    if (matched.isEmpty())
		throw new NoSuchMethodException();
	    Method mostSpecific = matched.get(0);
	    for (int i = 1; i < matched.size(); i++) {
		Method mi = matched.get(i);
		Result r = compareMethods(mi, mostSpecific);
		if (r == Result.LESS) {
		    mostSpecific = mi;
		}
	    }
	    for (int i = 0; i < matched.size(); i++) {
		Method mi = matched.get(i);
		Result r = compareMethods(mi, mostSpecific);
		if (r == Result.INCOMPARABLE) {
		    mostSpecific = null;
		}
	    }
	    if (mostSpecific == null)
		throw new NoSuchMethodException("Ambiguous call " + matched);
	    return mostSpecific;
	}
	
	boolean useless_search = false;
	
	public Object invoke(Object proxy, java.lang.reflect.Method m, Object[] args) throws java.lang.Throwable {
	    // Find the most specific method with the same name as m.
	    Class<?> c = target.getClass();
	    
	    Class[] argTypes = new Class[args.length];
	    for (int i = 0; i < args.length; i++) {
		argTypes[i] = args[i].getClass();
	    }

	    try {
		if (!useless_search) {
		    m = findMethod(c, m.getName(), m.getReturnType(), argTypes);
		}
		else {
		    ProxyCacheKey key = new ProxyCacheKey(m.getName(), argTypes);
		    Method cached = cache.get(key);
		    if (cached == null) {
			cached = findMethod(c, m.getName(), m.getReturnType(), argTypes);
			cache.put(key, cached);
		    }
		    m = cached;
		}
	    }
	    catch (NoSuchMethodException e) {
		throw e;
	    }

	    try {
		return m.invoke(target, args);
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
    }

    static interface Visitor {
	void visit(Node n);
    }

    static class Visitor1 implements Visitor {
	public void visit(Node n) {
	    node++;
//	    System.out.println("node");
	}
    }

    static class Expr extends Node {
    }

    static interface FunVisitor<S,T> {
	public S visit(T x);
    }

    static interface VoidVisitor<T> {
	public void visit(T x, Object... args);
    }

    static class VoidVisitor2 implements VoidVisitor<Node> {
	public void visit(Node n, Object... args) {
	    String s = "";
	    for (Object o : args) {
		s += " " + o;
	    }
	    System.out.println("node" + s);
	}
	public void visit(Add n, Object... args) {
	    String s = "";
	    for (Object o : args) {
		s += " " + o;
	    }
	    System.out.printf("add" + s);
	}
	public void visit(Sub n, Object... args) {
	    String s = "";
	    for (Object o : args) {
		s += " " + o;
	    }
	    System.out.printf("sub" + s);
	}
    }

    static class Node {
	void accept(Visitor v) {
	    CopyOfDispatch.proxy(Visitor.class, v).visit(this);

	    // Slower, but cleaner
//	    Dispatch.proxy(v).visit(this);
	}
	void accept(VoidVisitor<Node> v, Object... args) {
	    CopyOfDispatch.proxy(VoidVisitor.class, v).visit(this, args);
	}
    }
}
