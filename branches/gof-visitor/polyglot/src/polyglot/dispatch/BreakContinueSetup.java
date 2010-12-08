package polyglot.dispatch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import funicular.Clock;

import polyglot.ast.Branch_c;
import polyglot.ast.Case_c;
import polyglot.ast.Do_c;
import polyglot.ast.Expr_c;
import polyglot.ast.For_c;
import polyglot.ast.Labeled_c;
import polyglot.ast.LocalClassDecl_c;
import polyglot.ast.Node;
import polyglot.ast.Node_c;
import polyglot.ast.Switch_c;
import polyglot.ast.Term;
import polyglot.ast.Term_c;
import polyglot.ast.While_c;
import polyglot.frontend.Globals;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.Ref.Callable;
import polyglot.visit.NodeVisitor;

public class BreakContinueSetup {

    TypeSystem ts;
    Map<Node, Ref<Collection<Name>>> breaks;
    Map<Node, Ref<Collection<Name>>> continues;

    protected Clock jobClock() {
        return (Clock) Globals.currentJob().get("clock");
    }

    public BreakContinueSetup(TypeSystem ts, Map<Node, Ref<Collection<Name>>> breaks, Map<Node, Ref<Collection<Name>>> continues) {
	this.ts = ts;
	this.breaks = breaks;
	this.continues = continues;
    }

    public void visit(Node_c n, Node parent) {}

    Ref<Collection<Name>> breaksRef(Term n) {
	Ref<Collection<Name>> r = breaks.get(n);
	if (r == null) {
	    r = Types.<Collection<Name>> lazyRef(null);
	    breaks.put(n, r);
	}
	return r;
    }
    
    Ref<Collection<Name>> continuesRef(Term n) {
	Ref<Collection<Name>> r = continues.get(n);
	if (r == null) {
	    r = Types.<Collection<Name>> lazyRef(null);
	    continues.put(n, r);
	}
	return r;
    }

    public void equateInto(Ref<Boolean> a, final Ref<Boolean> b) {
	a.setResolver(new Callable<Boolean>() {
	    public Boolean call() {
		return b.get();
	    }
	}, jobClock());
    }

    private void noBreaks(Term n) {
	breaksRef(n).update(Collections.<Name> emptySet());
    }
    
    private void noContinues(Term n) {
	continuesRef(n).update(Collections.<Name> emptySet());
    }

    private void unionChildBreaks(final Term n) {
	unionChildBreaks(breaksRef(n), n);
    }
    private void unionChildContinues(final Term n) {
	unionChildContinues(continuesRef(n), n);
    }

    private void unionChildBreaks(Ref<Collection<Name>> r, final Term n) {
	r.setResolver(new Callable<Collection<Name>>() {
	    public Collection<Name> call() {
		final Set<Name> s = new HashSet<Name>();
		n.visitChildren(new NodeVisitor() {
		    @Override
		    public Node override(Node n) {
			if (n instanceof Term)
			    s.addAll(breaksRef((Term) n).get());
			return n;
		    }
		});
		return packSet(s);
	    }
	}, jobClock());
    }
    
    private void unionChildContinues(Ref<Collection<Name>> r, final Term n) {
	r.setResolver(new Callable<Collection<Name>>() {
	    public Collection<Name> call() {
		final Set<Name> s = new HashSet<Name>();
		n.visitChildren(new NodeVisitor() {
		    @Override
		    public Node override(Node n) {
			if (n instanceof Term)
			    s.addAll(continuesRef((Term) n).get());
			return n;
		    }
		});
		return packSet(s);
	    }
	}, jobClock());
    }

    public void visit(final Term_c n, Node parent) {
	noBreaks(n);
	noContinues(n);
    }

    public void visit(final Expr_c n, Node parent) {
	unionChildBreaks(n);
	unionChildContinues(n);
    }

    public void visit(LocalClassDecl_c n, Node parent) {
	noBreaks(n);
	noContinues(n);
    }

    public void visit(Labeled_c n, Node parent) {
	// A labeled statement can complete normally if at least one of the
	// following is true:
	// The contained statement can complete normally.
	// There is a reachable break statement that exits the labeled
	// statement.
	final Ref<Collection<Name>> b = breaksRef(n.statement());
	final Ref<Collection<Name>> c = continuesRef(n.statement());
	final Name labelId = n.labelNode().id();
	removeBreakLabel(n, b, labelId);
	removeContinueLabel(n, c, labelId);
    }

    private void removeBreakLabel(Term n, final Ref<Collection<Name>> b, final Name labelId) {
	breaksRef(n).setResolver(new Callable<Collection<Name>>() {
	    public Collection<Name> call() {
		Set<Name> ns = new HashSet<Name>(b.get());
		ns.remove(labelId);
		return packSet(ns);
	    }
	}, jobClock());
    }
    
    private void removeContinueLabel(Term n, final Ref<Collection<Name>> b, final Name labelId) {
	continuesRef(n).setResolver(new Callable<Collection<Name>>() {
	    public Collection<Name> call() {
		Set<Name> ns = new HashSet<Name>(b.get());
		ns.remove(labelId);
		return packSet(ns);
	    }
	}, jobClock());
    }

    public void visit(final Case_c n, Node parent) {
	noBreaks(n); noContinues(n);
    }

    public void visit(final Switch_c n, Node parent) {
	final Ref<Collection<Name>> b = Types.lazyRef(null);
	unionChildBreaks(b, n);
	removeBreakLabel(n, b, null);

	unionChildContinues(n);
    }

    public void visit(final While_c n, Node parent) {
	final Ref<Collection<Name>> b = breaksRef(n.body());
	final Ref<Collection<Name>> c = continuesRef(n.body());
	removeBreakLabel(n, b, null);
	removeContinueLabel(n, c, null);
    }

    public void visit(final Do_c n, Node parent) {
	final Ref<Collection<Name>> b = breaksRef(n.body());
	final Ref<Collection<Name>> c = continuesRef(n.body());
	removeBreakLabel(n, b, null);
	removeContinueLabel(n, c, null);
    }
    
    public void visit(final For_c n, Node parent) {
	final Ref<Collection<Name>> b = breaksRef(n.body());
	final Ref<Collection<Name>> c = continuesRef(n.body());
	removeBreakLabel(n, b, null);
	removeContinueLabel(n, c, null);
    }

    // A break, continue, return, or throw statement cannot complete normally.
    public void visit(final Branch_c n, Node parent) {
	switch (n.kind()) {
	case BREAK:
	    breaksRef(n).setResolver(new Callable<Collection<Name>>() {
		public Collection<Name> call() {
		    Name label = n.labelNode() != null ? n.labelNode().id() : null;
		    if (n.reachableRef().get())
			return Collections.<Name> singleton(label);
		    else
			return Collections.<Name> emptySet();
		}
	    }, jobClock());
	    noContinues(n);
	    break;
	case CONTINUE:
	    noBreaks(n);
	    continuesRef(n).setResolver(new Callable<Collection<Name>>() {
		public Collection<Name> call() {
		    Name label = n.labelNode() != null ? n.labelNode().id() : null;
		    if (n.reachableRef().get())
			return Collections.<Name> singleton(label);
		    else
			return Collections.<Name> emptySet();
		}
	    }, jobClock());
	    break;
	}
    }

    private static <T> Set<T> packSet(Set<T> ns) {
	if (ns.size() == 0)
	    return Collections.<T> emptySet();
	if (ns.size() == 1)
	    return Collections.<T> singleton(ns.iterator().next());
	return ns;
    }

}
