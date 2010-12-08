package polyglot.dispatch.dataflow;

import java.util.ArrayList;
import java.util.Collections;
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
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Do_c;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.For_c;
import polyglot.ast.If_c;
import polyglot.ast.Initializer_c;
import polyglot.ast.Labeled;
import polyglot.ast.Local;
import polyglot.ast.MethodDecl_c;
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
import polyglot.dispatch.ThrowTypes;
import polyglot.frontend.Globals;
import polyglot.types.Name;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Pair;
import polyglot.visit.NodeVisitor;

public abstract class DataFlow<I extends FlowItem<I>> {
    protected abstract I entryItem();
    protected abstract I initialItem();
    protected I unreachableItem() { return initialItem(); }

    boolean includeExceptionalPaths = true;
    boolean includeBooleanPaths = true;
    boolean includeExpressions = true;
    
    public I getItem(Node n) {
	return null;
    }
    
    public void putItem(Node n, I i) {
    }
    
    protected I accept(Node n, Node parent, I i) {
	if (n == null) {
	    return i;
	}
	
	putItem(n, i);
//	System.out.println("n = " + n + ": " + i);

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
	}

	return box.item;
    }

    void unconditionalEdge(Node fst, Node snd, I item) {
    }
    void valueEdge(Object v, Node fst, Node snd, I item) {
    }
    void throwEdge(Type t, Node fst, Node snd, I item) {
    }
    
    void recordThrow(Type t, Node thrower, I item) {
	Context<I> context = this.context;
	boolean caught = false;
	while (context != null && ! caught) {
	    if (context instanceof TryContext) {
		TryContext<I> tc = (TryContext<I>) context;
		Try tr = tc.n;
		for (int k = 0; k < tr.catchBlocks().size(); k++) {
		    Catch cb = tr.catchBlocks().get(k);
		    FlowItemRef<I> m = tc.catchTargets[k];
		    
		    if (Globals.TS().isSubtype(t, cb.catchType(), cb.context())) {
			// definite catch -- stop looking for a match
			m.post(item);
			caught = true;
			break;
		    }
		    else if (Globals.TS().isSubtype(cb.catchType(), t, cb.context())) {
			// possible catch -- keep looking
			m.post(item);
		    }
		}
	    }
	    else if (context instanceof RootContext) {
		RootContext<I> rc = (RootContext<I>) context;
		rc.target.post(item);
		caught = true;
		break;
	    }
	    context = context.outer;
	}
    }
    void recordBreak(Name label, Node breaker, I item) {
	Context<I> context = this.context;
	while (context != null) {
	    if (context instanceof BreakContinueContext) {
		BreakContinueContext<I> c = (BreakContinueContext<I>) context;
		if (label == null || (c.parent instanceof Labeled && ((Labeled) c.parent).labelNode().id() == label)) {
		    c.breakTarget.post(item);
		    break;
		}
	    }
	    if (context instanceof BreakContext) {
		BreakContext<I> c = (BreakContext<I>) context;
		if (label == null || (c.parent instanceof Labeled && ((Labeled) c.parent).labelNode().id() == label)) {
		    c.breakTarget.post(item);
		    break;
		}
		break;
	    }
	    context = context.outer;
	}
    }
    void recordContinue(Name label, Node continuer, I item) {
	Context<I> context = this.context;
	while (context != null) {
	    if (context instanceof BreakContinueContext) {
		BreakContinueContext<I> c = (BreakContinueContext<I>) context;
		if (label == null || (c.parent instanceof Labeled && ((Labeled) c.parent).labelNode().id() == label)) {
		    c.continueTarget.post(item);
		    break;
		}
		break;
	    }
	    context = context.outer;
	}
    }
    void recordReturn(Node returner, I item) {
	Context<I> context = this.context;
	while (context != null) {
	    if (context instanceof RootContext) {
		RootContext<I> c = (RootContext<I>) context;
		break;
	    }
	    context = context.outer;
	}
    }

    // Visit list of nodes sequentially.
    protected I acceptList(List<? extends Node> ns, Node prev, Node parent, I item) {
	I i = item;
	for (Node n : ns) {
	    if (prev == parent)
		;
	    else
		;
	    i = accept(n, parent, i);
	    prev = n;
	}
	return i;
    }

    protected I fieldAssign(Field left, Expr right, I item) {
	return item;
    }

    protected I localAssign(Local left, Expr right, I item) {
	return item;
    }

    protected I arrayAssign(ArrayAccess left, Expr right, I item) {
	return item;
    }

    public I visit(Term_c n, Node parent, I item) {
	TypeSystem ts = Globals.TS();
	for (Type t : n.<List<Type>>accept(new ThrowTypes(ts))) {
	    recordThrow(t, n, item);
	}
	return acceptChildren(n, item);
    }
    
    public I visit(MethodDecl_c n, Node parent, I item) {
	pushRoot(n, new FlowItemRef<I>(initialItem()));
	I i = acceptChildren(n, item);
	pop();
	return i;
    }
    public I visit(ConstructorDecl_c n, Node parent, I item) {
	pushRoot(n, new FlowItemRef<I>(initialItem()));
	I i = acceptChildren(n, item);
	pop();
	return i;
    }
    public I visit(FieldDecl_c n, Node parent, I item) {
	pushRoot(n, new FlowItemRef<I>(initialItem()));
	I i = acceptChildren(n, item);
	pop();
	return i;
    }
    public I visit(Initializer_c n, Node parent, I item) {
	pushRoot(n, new FlowItemRef<I>(initialItem()));
	I i = acceptChildren(n, item);
	pop();
	return i;
    }
    
    static class Context<I extends FlowItem<I>> { Context<I> outer; }
    static class TryContext<I extends FlowItem<I>> extends Context<I> { Try n;
    public FlowItemRef<I>[] catchTargets;
    public FlowItemRef<I> finallyTarget; }
    static class RootContext<I extends FlowItem<I>> extends Context<I> { Node n; FlowItemRef<I> target; }
    static class BreakContinueContext<I extends FlowItem<I>> extends BreakContext<I> { FlowItemRef<I> continueTarget; }
    static class BreakContext<I extends FlowItem<I>> extends Context<I> { Node parent; FlowItemRef<I> breakTarget; }
    
    Context<I> context;
    
    void push(Context c) {
	c.outer = context;
	context = c;
    }
    
    void pushRoot(Node n, FlowItemRef<I> target) {
	RootContext<I> c = new RootContext<I>();
	c.n = n;
	c.target = target;
	push(c);
    }
    void pushTry(Try n, FlowItemRef<I>[] catchTargets) {
	TryContext<I> c = new TryContext<I>();
	c.n = n;
	c.catchTargets = catchTargets;
	push(c);
    }
    void pushLoop(Node parent, FlowItemRef<I> breakTarget, FlowItemRef<I> continueTarget) {
	BreakContinueContext<I> c = new BreakContinueContext<I>();
	c.parent = parent;
	c.breakTarget = breakTarget;
	c.continueTarget = continueTarget;
	push(c);
    }
    void pushSwitch(Node parent, FlowItemRef<I> breakTarget) {
	BreakContext<I> c = new BreakContext<I>();
	c.parent = parent;
	c.breakTarget = breakTarget;
	push(c);
    }
    void pop() {
	context = context.outer;
    }
    
    public I visit(Try_c n, Node parent, I item) {
	List<FlowItemRef<I>> ms = new ArrayList<FlowItemRef<I>>(n.catchBlocks().size());
	for (Catch cb : n.catchBlocks()) {
	    ms.add(new FlowItemRef<I>(initialItem()));
	}
	
	pushTry(n, ms.toArray(new FlowItemRef[ms.size()]));

	I t = accept(n.tryBlock(), n, item);

	if (n.finallyBlock() != null) {
	    t = accept(n.finallyBlock(), n, t);
	}
	
	for (FlowItemRef<I> m : ms) {
	    m.reset();
	}
	
	I r = t;

	for (int k = 0; k < n.catchBlocks().size(); k++) {
	    Catch cb = n.catchBlocks().get(k);
	    FlowItemRef<I> m = ms.get(k);

	    I c = accept(cb, n, m.current());

	    if (n.finallyBlock() != null) {
		c = accept(n.finallyBlock(), n, c);
	    }

	    r = r.mergeWith(c);
	}

	pop();

	return r;
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
	
//	if (n.operator() == Binary.BIT_AND && n.type().isBoolean()) {
//	    I p = accept(n.left(), n, item);
//	    I q = accept(n.right(), n, p);
//	    return q;
//	}
//	if (n.operator() == Binary.BIT_OR && n.type().isBoolean()) {
//	    I p = accept(n.left(), n, item);
//	    I q = accept(n.right(), n, p);
//	    return q;
//	}

	return acceptChildren(n, item);
    }

    public I visit(Assign_c n, Node parent, I item) {
	Expr left = n.left();
	if (left instanceof Local) {
	    Local x = (Local) left;
	    I p = accept(n.right(), n, item);
	    I i = localAssign(x, n.right(), p);
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
	item = accept(n.expr(), n, item);

	I start = item;
	I prev = null;
	
	FlowItemRef<I> m = new FlowItemRef<I>(item);
	
	pushSwitch(parent, m);

	boolean hasDefault = false;

	for (SwitchElement e : n.elements()) {
	    if (e instanceof Case) {
		Case k = (Case) e;
		if (! k.isDefault())
		    valueEdge(k.value(), n.expr(), e, start);
		else {
		    hasDefault = true;
		}
		prev = prev != null ? prev.mergeWith(start) : start;
		hasDefault |= k.isDefault();
	    }
	    
	    I d = accept(e, n, prev);

	    prev = d;
	}

	if (! hasDefault) {
	    m.post(start);
	}
	
	pop();

	return m.current();
    }
    
    static class FlowItemRef<I extends FlowItem<I>> {
	I curr;
	boolean changed;
	
	FlowItemRef(I curr) {
	    this.curr = curr;
	    this.changed = true;
	}
	
	boolean post(I i) {
	    I old = this.curr;
	    I neu = old.mergeWith(i);
	    if (! old.equals(neu)) {
		this.curr = neu;
		this.changed = true;
		return true;
	    }
	    return false;
	}
	
	void reset() {
	    changed = false;
	}
	
	boolean changed() {
	    return changed;
	}
	
	I current() {
	    return curr;
	}
	
	public String toString() {
	    if (curr == null)
		return "null";
	    return curr.toString() + (changed ? " (changed)" : "");
	}
    }
    
    enum Endpoint {
	ENTRY, EXIT
    }

    public I visit(While_c n, Node parent, I item) {
	FlowItemRef<I> mb = new FlowItemRef<I>(item);
	FlowItemRef<I> me = new FlowItemRef<I>(initialItem());
	
	Node last = n;
	
	while (mb.changed() || me.changed()) {
	    mb.reset();
	    me.reset();
	    
	    I p = accept(n.cond(), n, mb.current());
	    me.post(p);
	    
	    pushLoop(n, me, mb);
	    
	    I q = accept(n.body(), n, p);
	
	    pop();
	    
	    mb.post(q);
	    
	    last = n.body();
	}
	
	return me.current();
    }

    public I visit(Do_c n, Node parent, I item) {
	FlowItemRef<I> mb = new FlowItemRef<I>(item);
	FlowItemRef<I> me = new FlowItemRef<I>(initialItem());
	
	while (mb.changed() || me.changed()) {
	    me.reset();
	    mb.reset();
	    
	    pushLoop(parent, me, mb);
	    
	    I p = accept(n.body(), n, mb.current());
	   
	    pop();
	    
	    me.post(p);
	    
	    I q = accept(n.cond(), n, me.current());
	    
	    mb.post(q);
	}

	return me.current();
    }

    public I visit(For_c n, Node parent, I item) {
	
	FlowItemRef<I> mb = new FlowItemRef<I>(initialItem());
	FlowItemRef<I> me = new FlowItemRef<I>(initialItem());

	I top = acceptList(n.inits(), n, n, item);
	
//	Node last = n.inits().size() > 0 ? n.inits().get(n.inits().size()-1) : n;
	
	while (mb.changed() || me.changed()) {
	    mb.reset();
	    me.reset();
	    
	    I p = accept(n.cond(), n, top);
	    me.post(p);
	    
	    pushLoop(n, me, mb);
	    
	    I q = accept(n.body(), n, me.current());
	
	    pop();
	    
	    mb.post(q);
	    
	    I r = acceptList(n.iters(), n.body(), n, mb.current());
	    
	    top = r;
	    
//	    last = n.body();
	}
	
	return me.current();
    }

}

