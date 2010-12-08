package polyglot.dispatch.dataflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import polyglot.ast.ArrayAccess;
import polyglot.ast.Assign_c;
import polyglot.ast.Binary;
import polyglot.ast.Binary_c;
import polyglot.ast.Branch_c;
import polyglot.ast.Case;
import polyglot.ast.Catch;
import polyglot.ast.ClassBody_c;
import polyglot.ast.Do_c;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.For_c;
import polyglot.ast.If_c;
import polyglot.ast.Labeled;
import polyglot.ast.Local;
import polyglot.ast.Node;
import polyglot.ast.Return_c;
import polyglot.ast.SwitchElement;
import polyglot.ast.Switch_c;
import polyglot.ast.Term;
import polyglot.ast.Term_c;
import polyglot.ast.Throw_c;
import polyglot.ast.Try;
import polyglot.ast.Try_c;
import polyglot.ast.Unary;
import polyglot.ast.Unary_c;
import polyglot.ast.While_c;
import polyglot.frontend.Globals;
import polyglot.types.Name;
import polyglot.types.Type;
import polyglot.util.Pair;
import polyglot.visit.NodeVisitor;

public abstract class CopyOfDataFlow<I extends FlowItem<I>> {
    protected abstract I entryItem();
    protected abstract I initialItem();
    protected I unreachableItem() { return initialItem(); }

    boolean includeExceptionalPaths = true;
    boolean includeBooleanPaths = true;
    boolean includeExpressions = true;

    protected I accept(Node n, Node parent, I i) {
	if (n == null) {
	    return i;
	}

	I j = n.accept(this, parent, i);
	return j;
    }

    // Visit children sequentially.
    protected I acceptChildren(final Node n, final I item) {
	if (n == null) {
	    return item;
	}

	class Box {
	    I item;
	    Node prev;
	    Box(Node p, I i) { prev = p; item = i; }
	}

	final Box box = new Box(n, item);

	n.visitChildren(new NodeVisitor() {
	    public Node override(Node child) {
		if (child instanceof Term) {
		    if (box.prev == n)
			normalEdge(n, child, box.item);
		    else
			normalEdge(box.prev, child, box.item);
		    box.item = accept(child, n, box.item);
		    box.prev = child;
		}
		return child;
	    }
	});
	
	if (box.prev == n) {
	    // no children
	}
	else {
	    normalEdge(box.prev, n, box.item);
	}

	return box.item;
    }

    Map<Type, List<Pair<Node, I>>> exceptions = new HashMap<Type, List<Pair<Node, I>>>();
    Map<Name, List<Pair<Node, I>>> breaks = new HashMap<Name, List<Pair<Node, I>>>();
    Map<Name, List<Pair<Node, I>>> continues = new HashMap<Name, List<Pair<Node, I>>>();
    List<Pair<Node, I>> returns = new ArrayList<Pair<Node, I>>();

    void normalEdge(Node fst, Node snd, I item) {
	System.out.println("next " + fst + " -> " + snd + " : " + item);
    }
    void booleanEdge(boolean v, Node fst, Node snd, I item) {
	System.out.println(v + " " + fst + " -> " + snd + " : " + item);
    }
    void trueEdge(Node fst, Node snd, I item) {
	booleanEdge(true, fst, snd, item);
    }
    void falseEdge(Node fst, Node snd, I item) {
	booleanEdge(false, fst, snd, item);
    }
    void throwEdge(Type t, Node fst, Node snd, I item) {
	System.out.println("throw[" + t + "] " + fst + " -> " + snd + " : " + item);
    }
    void recordThrow(Type t, Node thrower, I item) {
	List<Pair<Node, I>> l = exceptions.get(t);
	if (l == null) {
	    l = new ArrayList<Pair<Node, I>>(1);
	    exceptions.put(t, l);
	}
	l.add(new Pair(thrower, item));
	System.out.println("throw [" + t + "] " + thrower + " : " + item);
    }
    void recordBreak(Name label, Node breaker, I item) {
	List<Pair<Node, I>> l = breaks.get(label);
	if (l == null) {
	    l = new ArrayList<Pair<Node, I>>(1);
	    breaks.put(label, l);
	}
	l.add(new Pair(breaker, item));
	System.out.println("break [" + label + "] " + breaker + " : " + item);
    }
    void recordContinue(Name label, Node continuer, I item) {
	List<Pair<Node, I>> l = continues.get(label);
	if (l == null) {
	    l = new ArrayList<Pair<Node, I>>(1);
	    continues.put(label, l);
	}
	l.add(new Pair(continuer, item));
	System.out.println("continue [" + label + "] " + continuer + " : " + item);
    }
    void recordReturn(Node returner, I item) {
	returns.add(new Pair(returner, item));
	System.out.println("return " + returner + " : " + item);
    }

    // Visit list of nodes sequentially.
    protected I acceptList(List<? extends Node> ns, Node prev, Node parent, I item) {
	I i = item;
	for (Node n : ns) {
	    if (prev == parent)
		normalEdge(prev, n, i);
	    else
		normalEdge(prev, n, i);
	    i = accept(n, parent, i);
	    prev = n;
	}
	return i;
    }

    protected I fieldAssign(Field left, Expr right, I item) {
	normalEdge(right, left, item);
	return item;
    }

    protected I localAssign(Local left, Expr right, I item) {
	normalEdge(right, left, item);
	return item;
    }

    protected I arrayAssign(ArrayAccess left, Expr right, I item) {
	normalEdge(right, left, item);
	return item;
    }

    public I visit(Term_c n, Node parent, I item) {
	return acceptChildren(n, item);
    }
    
    class Context { Context outer; }
    class TryContext extends Context { Try n; }
    class LoopContext extends Context { Node parent; Node continueTarget; }
    class SwitchContext extends Context { Node parent; }
    
    Context context;
    
    void pushTry(Try n) {
	TryContext c = new TryContext();
	c.n = n;
	c.outer = context;
	context = c;
    }
    void pushLoop(Node parent, Node loop, Node continueTarget) {
	LoopContext c = new LoopContext();
	c.parent = parent;
	c.continueTarget = continueTarget;
	c.outer = context;
	context = c;
    }
    void pushSwitch(Node parent, Node switchStmt) {
	SwitchContext c = new SwitchContext();
	c.parent = parent;
	c.outer = context;
	context = c;
    }
    void pop() {
	context = context.outer;
    }

    public I visit(Try_c n, Node parent, I item) {
	pushTry(n);
	Map<Type, List<Pair<Node, I>>> oldExceptions = pushExceptions();

	normalEdge(n, n.tryBlock(), item);
	I t = accept(n.tryBlock(), n, item);

	if (n.finallyBlock() != null) {
	    normalEdge(n.tryBlock(), n.finallyBlock(), t);
	    t = accept(n.finallyBlock(), n, t);
	    normalEdge(n.finallyBlock(), n, t);
	}
	else {
	    normalEdge(n.tryBlock(), n, t);
	}
	
	I r = t;

	for (Catch cb: n.catchBlocks()) {
	    Type type = cb.formal().typeNode().typeRef().get();
	    I i = getThrowItem(cb, type);
	    I c = accept(cb, n, i);

	    if (n.finallyBlock() != null) {
		normalEdge(cb, n.finallyBlock(), c);
		c = accept(n.finallyBlock(), n, c);
		normalEdge(n.finallyBlock(), n, c);
	    }
	    else {
		normalEdge(cb, n, c);
	    }

	    r = r.mergeWith(c);

	    removeThrowItems(cb, type);
	}

	popExceptions(oldExceptions);
	pop();

	return r;
    }
    private void popBreaks(Map<Name, List<Pair<Node, I>>> oldBreaks) {
	Map<Name, List<Pair<Node, I>>> newBreaks = breaks;
	breaks = oldBreaks;
	for (Map.Entry<Name, List<Pair<Node, I>>> e : newBreaks.entrySet()) {
	    for (Pair<Node, I> p : e.getValue()) {
		recordBreak(e.getKey(), p.fst(), p.snd());
	    }
	}
    }
    private Map<Name, List<Pair<Node, I>>> pushBreaks() {
	Map<Name, List<Pair<Node, I>>> oldBreaks = breaks;
	breaks = new HashMap<Name, List<Pair<Node, I>>>();
	return oldBreaks;
    }
    private void popContinues(Map<Name, List<Pair<Node, I>>> oldContinues) {
	Map<Name, List<Pair<Node, I>>> newContinues = continues;
	continues = oldContinues;
	for (Map.Entry<Name, List<Pair<Node, I>>> e : newContinues.entrySet()) {
	    for (Pair<Node, I> p : e.getValue()) {
		recordContinue(e.getKey(), p.fst(), p.snd());
	    }
	}
    }
    private Map<Name, List<Pair<Node, I>>> pushContinues() {
	Map<Name, List<Pair<Node, I>>> oldContinues = continues;
	continues = new HashMap<Name, List<Pair<Node, I>>>();
	return oldContinues;
    }
    private void popExceptions(Map<Type, List<Pair<Node, I>>> oldExceptions) {
	Map<Type, List<Pair<Node, I>>> newExceptions = exceptions;
	exceptions = oldExceptions;
	for (Map.Entry<Type, List<Pair<Node, I>>> e : newExceptions.entrySet()) {
	    for (Pair<Node, I> p : e.getValue()) {
		recordThrow(e.getKey(), p.fst(), p.snd());
	    }
	}
    }
    private Map<Type, List<Pair<Node, I>>> pushExceptions() {
	Map<Type, List<Pair<Node, I>>> oldExceptions = exceptions;
	exceptions = new HashMap<Type, List<Pair<Node, I>>>();
	return oldExceptions;
    }

    public I visit(ClassBody_c n, Node parent, I item) {
	// Do not recurse!
	return item;
    }

    public I visit(Unary_c n, Node parent,  I item) {
	if (n.operator() == Unary.PRE_DEC || n.operator() == Unary.PRE_INC || n.operator() == Unary.POST_DEC || n.operator() == Unary.POST_INC) {
	    return accept(n.expr(), n, item);
	}
	if (n.operator() == Unary.NOT) {
	    I i = accept(n.expr(), n, item);
	    return i;
	}
	return accept(n.expr(), n, item);
    }

    public I visit(Binary_c n, Node parent, I item) {
	if (n.operator() == Binary.COND_AND) {
	    I p = accept(n.left(), n, item);
	    I q = accept(n.right(), n, p);
	    I r = p.mergeWith(q);
	    return r;
	}
	if (n.operator() == Binary.COND_OR) {
	    I p = accept(n.left(), n, item);
	    I q = accept(n.right(), n, p);
	    I r = p.mergeWith(q);
	    return r;
	}
	if (n.operator() == Binary.BIT_AND && n.type().isBoolean()) {
	    I p = accept(n.left(), n, item);
	    I q = accept(n.right(), n, p);
	    return q;
	}
	if (n.operator() == Binary.BIT_OR && n.type().isBoolean()) {
	    I p = accept(n.left(), n, item);
	    I q = accept(n.right(), n, p);
	    return q;
	}
	I p = accept(n.left(), n, item);
	I q = accept(n.right(), n, p);
	return q;
    }

    public I visit(Assign_c n, Node parent, I item) {
	Expr left = n.left();
	if (left instanceof Local) {
	    Local x = (Local) left;
	    normalEdge(n, n.right(), item);
	    I p = accept(n.right(), n, item);
	    I i = localAssign(x, n.right(), p);
	    normalEdge(x, n, i);
	    return i;
	}
	if (left instanceof Field) {
	    Field f = (Field) left;
	    I p = accept(f.target(), f, item);
	    I q = accept(n.right(), n, p);
	    I i = fieldAssign(f, n.right(), q);
	    return i;
	}
	if (left instanceof ArrayAccess) {
	    ArrayAccess ai = (ArrayAccess) left;
	    I p = accept(ai.array(), ai, item);
	    I q = accept(ai.index(), ai, p);
	    I r = accept(n.right(), n, q);
	    I i = arrayAssign(ai, n.right(), r);
	    return i;
	}
	return item;
    }

    public I visit(If_c n, Node parent, I item) {
	if (n.alternative() != null) {
	    I p = accept(n.cond(), n, item);
	    I q = accept(n.consequent(), n, p);
	    I r = accept(n.alternative(), n, p);
	    return q.mergeWith(r);
	}
	else {
	    I p = accept(n.cond(), n, item);
	    I q = accept(n.consequent(), n, p);
	    return p.mergeWith(q);
	}
    }


    I getBranchItem(Node parent, Map<Name,List<Pair<Node, I>>> map) {
	if (map == null)
	    return null;
	I i = null;
	List<Pair<Node, I>> l = map.get(null);
	if (l == null) l = Collections.emptyList();
	for (Pair<Node, I> p : l) {
	    i = i != null ? i.mergeWith(p.snd()) : p.snd();
	}
	if (parent instanceof Labeled) {
	    Labeled ln = (Labeled) parent;
	    List<Pair<Node, I>> li = map.get(ln.labelNode().id());
	    if (li == null) li = Collections.emptyList();
	    for (Pair<Node, I> p : li) {
		i = i != null ? i.mergeWith(p.snd()) : p.snd();
	    }
	}
	return i;
    }

    I getThrowItem(Node n, Type type) {
	I i = initialItem();
	for (Map.Entry<Type, List<Pair<Node, I>>> e : exceptions.entrySet()) {
	    Type t = e.getKey();
	    List<Pair<Node, I>> l = e.getValue();

	    if (Globals.TS().isSubtype(type, t, n.context()) || Globals.TS().isSubtype(t, type, n.context())) {
		for (Pair<Node, I> p : l) {
		    I j = p.snd();
		    i = i.mergeWith(j);
		}
	    }
	}
	return i;
    }

    void removeThrowItems(Catch n, Type type) {
	for (Iterator<Map.Entry<Type, List<Pair<Node, I>>>> i = exceptions.entrySet().iterator(); i.hasNext(); ) {
	    Map.Entry<Type, List<Pair<Node, I>>> e = i.next();
	    Type t = e.getKey();
	    if (Globals.TS().isSubtype(t, type, n.context())) {
		i.remove();
	    }
	}
    }

    public I visit(Branch_c n, Node parent, I item) {
	switch (n.kind()) {
	case BREAK:
	    recordBreak(n.labelNode() != null ? n.labelNode().id() : null, n, item);
	    break;
	case CONTINUE:
	    recordContinue(n.labelNode() != null ? n.labelNode().id() : null, n, item);
	    break;
	}
	return unreachableItem();
    }
    public I visit(Throw_c n, Node parent, I item) {
	I i = accept(n.expr(), n, item);
	recordThrow(n.expr().type(), n, i);
	return unreachableItem();
    }
    public I visit(Return_c n, Node parent, I item) {
	if (n.expr() != null) {
	    I i = accept(n.expr(), n, item);
	    recordReturn(n, i);
	}
	return unreachableItem();
    }

    public I visit(Switch_c n, Node parent, I item) {
	Map<Name, List<Pair<Node, I>>> oldBreaks = pushBreaks();

	item = accept(n.expr(), n, item);

	I start = item;
	I prev = null;
	I end = null;
	
	pushSwitch(parent, n);

	boolean hasDefault = false;

	for (SwitchElement e : n.elements()) {
	    if (e instanceof Case) {
		prev = prev != null ? prev.mergeWith(start) : start;
		hasDefault |= ((Case) e).isDefault();
	    }

	    I d = accept(e, n, prev);

	    if (d != null) {
		end = end != null ? end.mergeWith(d) : d;
	    }
	    else {
		// breaks!
	    }

	    I b = getBranchItem(parent, breaks);
	    if (b != null)
		end = end != null ? end.mergeWith(b) : b;

		prev = d;
	}

	if (! hasDefault) {
	    end = end != null ? end.mergeWith(item) : end;
	}

	breaks.remove(null);
	popBreaks(oldBreaks);
	
	pop();

	return end;
    }

    public I visit(While_c n, Node parent, I item) {
	Map<Name, List<Pair<Node, I>>> oldBreaks = pushBreaks();
	Map<Name, List<Pair<Node, I>>> oldContinues = pushContinues();

	I neu = item;
	I out;
	I old;
	do {
	    old = neu;
	    I p = accept(n.cond(), n, old);
	    
	    pushLoop(parent, n, n.cond());
	    
	    I q = accept(n.body(), n, p);
	    
	    pop();
	    
	    // Set for a loop back edge.
	    I c = getBranchItem(parent, continues);
	    if (c != null)
		neu = q.mergeWith(c);
	    else
		neu = q;

	    I b = getBranchItem(parent, breaks);
	    if (b != null)
		out = p.mergeWith(b);
	    else
		out = p;
	} while (! neu.equals(old));

	breaks.remove(null);
	popBreaks(oldBreaks);
	continues.remove(null);
	popContinues(oldContinues);

	return out;
    }

    public I visit(Do_c n, Node parent, I item) {
	Map<Name, List<Pair<Node, I>>> oldBreaks = pushBreaks();
	Map<Name, List<Pair<Node, I>>> oldContinues = pushContinues();

	I neu = item;
	I out;
	I old;
	do {
	    old = neu;
	    
	    pushLoop(parent, n, n.body());
	    
	    I p = accept(n.body(), n, old);
	   
	    pop();
	    
	    // Set for a loop back edge.
	    I c = getBranchItem(parent, continues);
	    I q;
	    if (c != null)
		q = p.mergeWith(c);
	    else
		q = p;
	    neu = accept(n.cond(), n, q);

	    I b = getBranchItem(parent, breaks);
	    if (b != null)
		return neu.mergeWith(b);
	    else
		out = neu;
	} while (! neu.equals(old));

	breaks.remove(null);
	popBreaks(oldBreaks);
	continues.remove(null);
	popContinues(oldContinues);

	return out;
    }

    public I visit(For_c n, Node parent, I item) {
	Map<Name, List<Pair<Node, I>>> oldBreaks = pushBreaks();
	Map<Name, List<Pair<Node, I>>> oldContinues = pushContinues();

	I neu = acceptList(n.inits(), n, n, item);
	I out;
	I old;
	do {
	    old = neu;
	    I p = accept(n.cond(), n, old);
	    
	    pushLoop(parent, n, n.iters().size() > 0 ? n.iters().iterator().next() : (n.cond() != null ? n.cond() : n.body()));
	    I q = accept(n.body(), n, p);
	    pop();
	    
	    // Set for a loop back edge.
	    I c = getBranchItem(parent, continues);
	    I k;
	    if (c != null)
		k = q.mergeWith(c);
	    else
		k = q;

	    I b = getBranchItem(parent, breaks);
	    if (b != null)
		out = p.mergeWith(b);
	    else
		out = p;

	    neu = acceptList(n.iters(), n.body(), n, k);
	} while (! neu.equals(old));

	breaks.remove(null);
	popBreaks(oldBreaks);
	continues.remove(null);
	popContinues(oldContinues);

	return out;
    }

}

