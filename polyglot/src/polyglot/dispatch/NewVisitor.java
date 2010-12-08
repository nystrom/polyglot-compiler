package polyglot.dispatch;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import polyglot.ast.Expr;
import polyglot.ast.Node;

@Retention(RetentionPolicy.CLASS)
@interface DispatchArg {
    int arg();
}
interface NV {
    @DispatchArg(arg=1)
    public Node visit$dispatch(Node parent, Node n);    
}
public class NewVisitor implements NV {
    public Node visit$dispatch(Node parent, Node n) {
	return (Node) new Dispatch.Dispatcher("visit").invoke(this, parent, n);
    }
    public Node visit(Node parent, Expr n) {
	return n;
    }
}

class DispatchMaker {
    <T> T make(Class<T> x) throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException {
	Class<? extends Object> c = x.getClass();
	for (Method m : c.getMethods()) {
	    if (m.isAnnotationPresent(DispatchArg.class)) {
//		Annotation a = m.getAnnotation(DispatchArg.class);
//		Class ca =	a.getClass();
//		Method ma = ca.getMethod("arg");
//		Object o = ma.getDefaultValue();
		
		return Dispatch.proxy(x, x.newInstance());
	    }
	}
	return x.newInstance();
    }
}