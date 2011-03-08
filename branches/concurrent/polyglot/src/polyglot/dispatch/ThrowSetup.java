package polyglot.dispatch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import funicular.Clock;

import polyglot.ast.Branch_c;
import polyglot.ast.Case_c;
import polyglot.ast.Catch;
import polyglot.ast.Catch_c;
import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassDecl_c;
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Expr_c;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.Formal_c;
import polyglot.ast.Initializer_c;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.Node;
import polyglot.ast.Node_c;
import polyglot.ast.Term;
import polyglot.ast.Term_c;
import polyglot.ast.Try_c;
import polyglot.ast.TypeNode_c;
import polyglot.frontend.Globals;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Ref.Callable;
import polyglot.util.InternalCompilerError;
import polyglot.visit.NodeVisitor;

public class ThrowSetup {

    TypeSystem ts;
    Map<Node, Ref<Collection<Type>>> exceptions;

    public ThrowSetup(TypeSystem ts, Map<Node, Ref<Collection<Type>>> exceptions) {
	this.exceptions = exceptions;
	this.ts = ts;
    }
    
    protected funicular.Clock jobClock() {
        return (funicular.Clock) Globals.currentJob().get("clock");
    }

    public void visit(Node_c n, Node parent) {}

    public void visit(Term_c n, Node parent) {
	throw new InternalCompilerError("Missing case for " + n.getClass().getName());
    }

    public void visit(TypeNode_c n, Node parent) {
	noThrows(n);
    }

    public void visit(Formal_c n, Node parent) {
	noThrows(n);
    }

    public void visit(ClassDecl_c n, Node parent) {
	noThrows(n);
    }

    public void visit(ClassBody_c n, Node parent) {
	noThrows(n);
    }

    Ref<Collection<Type>> throwsRef(Term n) {
	return n.throwsRef();
//	Ref<Collection<Type>> r = exceptions.get(n);
//	if (r == null) {
//	    r = Types.<Collection<Type>> lazyRef(null);
//	    exceptions.put(n, r);
//	}
//	return r;
    }

    public void equateInto(Ref<Boolean> a, final Ref<Boolean> b) {
	a.setResolver(new Callable<Boolean>() {
	    public Boolean call() {
		return b.get();
	    }
	}, jobClock());
    }

    private void noThrows(Term n) {
	throwsRef(n).update(ts.uncheckedExceptions());
    }

    private void unionChildThrows(final Term n) {
	throwsRef(n).setResolver(new Callable<Collection<Type>>() {
	    public Collection<Type> call() {
		if (! n.reachableRef().get())
		    return Collections.emptySet();
		final Set<Type> s = new HashSet<Type>(ts.uncheckedExceptions());
		n.visitChildren(new NodeVisitor() {
		    @Override
		    public Node override(Node n) {
			if (n instanceof Term)
			    s.addAll(throwsRef((Term) n).get());
			return n;
		    }
		});
		if (n instanceof Term) {
		    Collection<Type> ss = n.accept(new ThrowTypes(ts));
		    s.addAll(ss);
		}
		return packSet(s);
	    }
	}, jobClock());
    }

    public void visit(final MethodDecl_c n, Node parent) {
	unionChildThrows(n);
    }

    public void visit(final Initializer_c n, Node parent) {
	unionChildThrows(n);
    }

    public void visit(final FieldDecl_c n, Node parent) {
	unionChildThrows(n);
    }

    public void visit(final ConstructorDecl_c n, Node parent) {
	unionChildThrows(n);
    }

    public void visit(final Expr_c n, Node parent) {
	unionChildThrows(n);
    }

    public void visit(final Case_c n, Node parent) {
	noThrows(n);
    }

    public void visit(final Branch_c n, Node parent) {
	noThrows(n);
    }

    public void visit(final Catch_c n, Node parent) {
	unionChildThrows(n);
    }

    public void visit(final Try_c n, Node parent) {
	throwsRef(n).setResolver(
			new Callable<Collection<Type>>() {
	    public Collection<Type> call() {
		if (! n.reachableRef().get())
		    return Collections.emptySet();
		Collection<Type> types = throwsRef(n.tryBlock()).get();
		Set<Type> ss = new HashSet<Type>();
		ss.addAll(ts.uncheckedExceptions());
		ss.addAll(types);
		for (Catch cb : n.catchBlocks()) {
		    Type ct = cb.catchType();
		    for (Type t : types) {
			if (ts.isSubtype(t, ct, n.context())) { //saurako: changed ct, t to t, ct
			    ss.remove(t);
			}
		    }
		    Collection<Type> t2 = throwsRef(cb).get(); 
			ss.addAll(t2);
		}
		return packSet(ss);
	    }
	}, jobClock());
    }

    private static <T> Set<T> packSet(Set<T> ns) {
	if (ns.size() == 0)
	    return Collections.<T> emptySet();
	if (ns.size() == 1)
	    return Collections.<T> singleton(ns.iterator().next());
	return ns;
    }

}
