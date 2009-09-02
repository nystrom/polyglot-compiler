package polyglot.dispatch;

import java.util.*;

import polyglot.ast.*;
import polyglot.types.*;

public class OldSimpleDataFlow {

    TypeSystem ts;

    /**
     * An <code>Item</code> contains the data which flows during the dataflow
     * analysis. Each
     * node in the flow graph will have two items associated with it: the input
     * item, and the output item, which results from calling flow with the
     * input item. The input item may itself be the result of a call to the 
     * confluence method, if many paths flow into the same node.
     * 
     * NOTE: the <code>equals(Item)</code> method and <code>hashCode()</code>
     * method must be implemented to ensure that the dataflow algorithm works
     * correctly.
     */
    public static abstract class Item {
	public abstract boolean equals(Object i);
	public abstract int hashCode();
	public abstract Item meetWith(Item i);
    }

    protected static class ReachItem extends Item {
	boolean completes;
	boolean reachable;

	ReachItem(boolean completes, boolean reachable) {
	    this.completes = completes;
	    this.reachable = reachable;
	}

	@Override
	public boolean equals(Object o) {
	    if (o instanceof ReachItem) {
		ReachItem i = (ReachItem) o;
		return completes == i.completes && reachable == i.reachable;
	    }
	    return false;
	}

	@Override
	public int hashCode() {
	    return (completes ? 17 : 19) + (reachable ? 23 : 29);
	}

	@Override
	public Item meetWith(Item i) {
	    ReachItem r = (ReachItem) i;
	    ReachItem r1 = this;
	    ReachItem r2 = r;
	    return item(r1.completes || r2.completes, r1.reachable || r2.reachable);
	}
    }
    
    static ReachItem[] items = 	new ReachItem[] { new ReachItem(false, false),
                               	                  new ReachItem(false, true),
                               	                  new ReachItem(true, false),
                               	                  new ReachItem(true, true) };

    /**
     * Create an initial Item for the term node. This is generally how the Item
     * that will be given to the start node of a graph is created, although this
     * method may also be called for other (non-start) nodes.
     * 
     * @return a (possibly null) Item.
     */
    protected Item createInitialItem(Node n, boolean entry) {
	return item(true, true);
    }

    public OldSimpleDataFlow(TypeSystem ts) {
	super();
	this.ts = ts;
    }

    public void visit(AmbAssign n) {
	assert false : "unimplemented node " + n;
    }

    public void visit(AmbExpr n) {
	assert false : "unimplemented node " + n;
    }

    public void visit(AmbPrefix n) {
	assert false : "unimplemented node " + n;
    }

    public void visit(AmbQualifierNode n) {
	assert false : "unimplemented node " + n;
    }

    public void visit(AmbReceiver n) {
	assert false : "unimplemented node " + n;
    }

    public void visit(AmbTypeNode n) {
	assert false : "unimplemented node " + n;
    }

    Map<Node,Item> inflows = new HashMap<Node,Item>();
    Map<Node,Item> outflows = new HashMap<Node,Item>();

    public final Item meet(Item i1, Item i2) {
	return i1.meetWith(i2);
    }
    
    static ReachItem item(boolean completes, boolean reachable) {
	return items[(completes ? 2 : 0) | (reachable ? 1 : 0)];
    }

    public Item transfer(Node fst, Node snd, Flow flow, Item inItem) {
	ReachItem r = (ReachItem) inItem;
	System.out.println("flow: " + fst + " -> " + snd);
	if (fst instanceof Branch || fst instanceof Throw || fst instanceof Return)
	    return item(false, r.completes && r.reachable);
	else
	    return item(r.completes, r.completes && r.reachable);
    }

    public final void flow(Node fst, Node snd, Flow flow) {
	Item fstOut = outflows.get(fst);
	if (fstOut == null) {
	    fstOut = createInitialItem(fst, false);
	}

	boolean inChanged = false;

	Item sndIn = inflows.get(snd);
	if (sndIn == null) {
	    sndIn = createInitialItem(snd, false);
	    inChanged = true;
	}

	Item newSndIn = meet(fstOut, sndIn);
	if (! newSndIn.equals(sndIn)) {
	    inChanged = true;
	}

	Item sndOut = outflows.get(snd);
	if (! inChanged && sndOut != null) {
	    return;
	}

	inflows.put(snd, newSndIn);

	Item newSndOut = transfer(fst, snd, flow, newSndIn);

	outflows.put(snd, newSndOut);

	// Now, recurse on the children.
	snd.accept(this);
    }

    public void visit(NodeList n) {
	Node prev = visitList(n, FlowDown, n.nodes());
	if (prev != n) {
	    flow(prev, n, FlowUp);
	}
	else {
	    leaf(n);
	}
    }

    public void visit(ArrayAccess n) {
	assert false : "unimplemented node " + n;
    }

    public void visit(ArrayAccessAssign n) {
	assert false : "unimplemented node " + n;
    }

    public void visit(ArrayInit n) {
	assert false : "unimplemented node " + n;
    }

    public void visit(ArrayTypeNode n) {
	assert false : "unimplemented node " + n;
    }

    static class Flow {}
    static class NormalFlow extends Flow {}
    static class AbruptFlow extends Flow {}
    static class ReturnFlow extends Flow {}

    abstract static class FinallyFlow extends Flow {
	Node src;
	Flow flow;

	public FinallyFlow(Node src, Flow flow) {
	    this.src = src;
	    this.flow = flow;
	}

	public boolean equals(Object o) {
	    if (o instanceof FinallyFlow) {
		FinallyFlow f = (FinallyFlow) o;
		return src == f.src && flow.equals(f.flow);
	    }
	    return false;
	}

	public int hashCode() {
	    return System.identityHashCode(src) + flow.hashCode();
	}
    }

    static class FinallyExitFlow extends FinallyFlow {

	public FinallyExitFlow(Node src, Flow flow) {
	    super(src, flow);
	}
    }

    static class FinallyEnterFlow extends FinallyFlow {
	public FinallyEnterFlow(Node src, Flow flow) {
	    super(src, flow);
	    // TODO Auto-generated constructor stub
	}
    }

    static class ExceptionalFlow extends Flow {
	Type type;

	public ExceptionalFlow(Type t) {
	    this.type = t;
	}
    }

    static class ConditionalFlow extends Flow {}

    static final Flow FlowStart = new NormalFlow();
    static final Flow FlowDown = new NormalFlow();
    static final Flow FlowUp = new NormalFlow();
    static final Flow True = new ConditionalFlow();
    static final Flow False = new ConditionalFlow();
    static final Flow JoinTrue = new NormalFlow();
    static final Flow JoinFalse = new NormalFlow();
    static final Flow FlowNext = new NormalFlow();
    static final Flow CaseSplit = new NormalFlow();
    static final Flow CaseJoin = new NormalFlow();
    static final Flow Break = new AbruptFlow();
    static final Flow Continue = new AbruptFlow();
    static final Flow Return = new ReturnFlow();

    public void visit(Assert n) {
	flow(n, n.cond(), FlowDown);
	n.cond().accept(this);
	if (n.errorMessage() != null) {
	    flow(n.cond(), n.errorMessage(), FlowNext);
	    n.errorMessage().accept(this);
	    flow(n.errorMessage(), n, FlowUp);
	}
	else {
	    flow(n.cond(), n, FlowUp);
	}
    }

    public void visit(Assign n) {
	assert false : "unimplemented node " + n;

    }

    private Node visitList(Node n, Flow firstFlow, List<? extends Node> children) {
	Node prev = n;
	Flow flow = firstFlow;
	for (Node s : children) {
	    flow(prev, s, flow);
	    flow = FlowNext;
	    prev = s;
	    s.accept(this);
	}
	return prev;
    }

    public void visit(Binary n) {
	if (n.operator() == Binary.COND_AND) {
	    flow(n, n.left(), FlowDown);
	    flow(n.left(), n.right(), True);
	    flow(n.left(), n, False);
	    flow(n.right(), n, FlowUp);
	}
	else if (n.operator() == Binary.COND_OR) {
	    flow(n, n.left(), FlowDown);
	    flow(n.left(), n.right(), False);
	    flow(n.left(), n, True);
	    flow(n.right(), n, FlowUp);
	}
	else {
	    flow(n, n.left(), FlowDown);
	    flow(n.left(), n.right(), FlowNext);
	    flow(n.right(), n, FlowUp);
	    if (n.operator() == Binary.DIV || n.operator() == Binary.MOD) {
		if (n.type().isLongOrLess())
		    exceptionalFlows(n, ts.ArithmeticException(), ts.emptyContext());
	    }
	}
    }

    public void visit(Block n) {
	Node prev = visitList(n, FlowDown, n.statements());
	if (prev != n) {
	    flow(prev, n, FlowUp);
	}
	else {
	    leaf(n);
	}
    }

    public void leaf(Node n) {
	System.out.println("<<<" + n + ">>>");
    }

    public void visit(BooleanLit n) {
	leaf(n);
    }

    Map<Name, Node> breakTargets = new HashMap<Name, Node>();
    Map<Name, Node> continueTargets = new HashMap<Name, Node>();
    Name NULL = Name.makeFresh();

    Node breakTarget(Name label) {
	if (label == null)
	    label = NULL;
	return breakTargets.get(label);
    }

    void breakTarget(Name label, Node n) {
	if (label == null)
	    label = NULL;
	if (n == null)
	    breakTargets.remove(label);
	else
	    breakTargets.put(label, n);
    }

    Node continueTarget(Name label) {
	if (label == null)
	    label = NULL;
	return continueTargets.get(label);
    }

    void continueTarget(Name label, Node n) {
	if (label == null)
	    label = NULL;
	if (n == null)
	    continueTargets.remove(label);
	else
	    continueTargets.put(label, n);
    }

    static Name name(Id n) {
	if (n == null)
	    return null;
	else
	    return n.id();
    }
    
    public void visit(Branch n) {
	if (n.kind() == Branch.BREAK) {
	    flow(n, breakTarget(name(n.labelNode())), Break);
	}
	else {
	    flow(n, continueTarget(name(n.labelNode())), Continue);
	}
    }

    public void visit(Call n) {
	if (n.target() != null) {
	    flow(n, n.target(), FlowDown);
	    Node prev = visitList(n.target(), FlowNext, n.arguments());
	    flow(prev, n, FlowUp);
	}
	else {
	    Node prev = visitList(n, FlowDown, n.arguments());
	    flow(prev, n, FlowUp);
	}
	if (n.target() instanceof Expr)
	    exceptionalFlows(n.target(), ts.NullPointerException(), ts.emptyContext());
	for (Type t : n.throwTypes(ts)) {
	    exceptionalFlows(n, t, ts.emptyContext());
	}
    }

    public void visit(CanonicalTypeNode n) {
	leaf(n);
    }

    public void visit(Case n) {
	flow(n, n.expr(), FlowDown);
	flow(n.expr(), n, FlowUp);
    }

    public void visit(Cast n) {
	flow(n, n.castType(), FlowDown);
	flow(n.castType(), n.expr(), FlowNext);
	flow(n.expr(), n, FlowUp);
    }

    public void visit(Catch n) {
	flow(n, n.formal(), FlowDown);
	flow(n.formal(), n.body(), FlowNext);
	flow(n.body(), n, FlowUp);
    }

    public void visit(CharLit n) {
	leaf(n);
    }

    public void visit(ClassBody n) {
	leaf(n);
    }

    public void visit(ClassDecl n) {
	embedded(n);
    }

    public void visit(ClassLit n) {
	flow(n, n.typeNode(), FlowDown);
	flow(n.typeNode(), n, FlowUp);
    }

    public void visit(Conditional n) {
	flow(n, n.cond(), FlowDown);
	flow(n.cond(), n.consequent(), True);
	flow(n.consequent(), n, JoinTrue);
	flow(n.cond(), n.alternative(), False);
	flow(n.alternative(), n, JoinFalse);
    }

    public void visit(ConstructorCall n) {
	if (n.qualifier() != null) {
	    flow(n, n.qualifier(), FlowDown);
	    Node prev = visitList(n.qualifier(), FlowNext, n.arguments());
	    flow(prev, n, FlowUp);
	}
	else {
	    Node prev = visitList(n, FlowDown, n.arguments());
	    flow(prev, n, FlowUp);
	}
    }

    public void visit(ConstructorDecl n) {
	embedded(n);
    }

    public void visit(Do n) {
	Node oldBreak = breakTarget(null);
	Node oldContinue = continueTarget(null);
	breakTarget(null, n);
	continueTarget(null, n.body());
	flow(n, n.body(), FlowDown);
	flow(n.body(), n.cond(), FlowNext);
	flow(n.cond(), n.body(), True);
	flow(n.cond(), n, False);
	breakTarget(null, oldBreak);
	continueTarget(null, oldContinue);
    }

    public void visit(Empty n) {
	leaf(n);
    }

    public void visit(Eval n) {
	flow(n, n.expr(), FlowDown);
	flow(n.expr(), n, FlowUp);
    }

    public void visit(Field n) {
	flow(n, n.target(), FlowDown);
	if (n.target() instanceof Expr)
	    exceptionalFlows(n.target(), ts.NullPointerException(), ts.emptyContext());
	flow(n.target(), n, FlowUp);
    }

    public void visit(FieldAssign n) {
	flow(n, n.target(), FlowDown);
	flow(n.target(), n.right(), FlowNext);
	if (n.target() instanceof Expr)
	    exceptionalFlows(n.target(), ts.NullPointerException(), ts.emptyContext());
	flow(n.right(), n, FlowUp);
    }

    public void visit(FieldDecl n) {
	embedded(n);
    }

    public void visit(FloatLit n) {
	leaf(n);
    }

    static Node first(List<? extends Node> ns, Node n1, Node n2) {
	if (ns.size() > 0)
	    return ns.get(0);
	if (n1 != null)
	    return n1;
	return n2;
    }
    
    public void visit(For n) {
	Node oldBreak = breakTarget(null);
	Node oldContinue = continueTarget(null);
	breakTarget(null, n);
	continueTarget(null, first(n.iters(), n.cond(), n.body()));

	Node prev = visitList(n, FlowDown, n.inits());
	
	if (n.cond() != null) {
	    if (prev == n)
		flow(n, n.cond(), FlowDown);
	    else
		flow(prev, n.cond(), FlowNext);
	    flow(n.cond(), n.body(), True);
	    flow(n.cond(), n, False);
	}
	else {
	    if (prev == n)
		flow(n, n.body(), FlowDown);
	    else
		flow(prev, n.body(), FlowNext);
	}
	
	flow(n.body(), first(n.iters(), n.cond(), n.body()), FlowNext);

	breakTarget(null, oldBreak);
	continueTarget(null, oldContinue);
    }

    public void visit(Formal n) {
	leaf(n);
    }

    public void visit(Id n) {
	leaf(n);
    }

    public void visit(If n) {
	flow(n, n.cond(), FlowDown);
	flow(n.cond(), n.consequent(), True);
	flow(n.consequent(), n, JoinTrue);
	if (n.alternative() != null) {
	    flow(n.cond(), n.alternative(), False);
	    flow(n.alternative(), n, JoinFalse);
	}
    }

    public void visit(Import n) {
	leaf(n);
    }

    public void visit(Initializer n) {
	leaf(n);
    }

    public void visit(Instanceof n) {
	flow(n, n.expr(), FlowDown);
	flow(n.expr(), n.compareType(), FlowNext);
	flow(n.compareType(), n, FlowUp);
    }

    public void visit(IntLit n) {
	leaf(n);
    }

    Node continueTargetOfNode(Node n) {
	if (n instanceof Loop) {
	    return ((Loop) n).continueTarget();
	}
	return n;
    }

    public void visit(Labeled n) {
	breakTarget(name(n.labelNode()), n);
	continueTarget(name(n.labelNode()), continueTargetOfNode(n.statement()));
	flow(n, n.statement(), FlowDown);
	flow(n.statement(), n, FlowUp);
	breakTarget(name(n.labelNode()), null);
	continueTarget(name(n.labelNode()), null);
    }

    public void visit(Local n) {
	leaf(n);
    }

    public void visit(LocalAssign n) {
	flow(n, n.local(), FlowDown);
	flow(n.local(), n.right(), FlowNext);
	flow(n.right(), n, FlowUp);
    }

    public void visit(LocalClassDecl n) {
	flow(n, n.decl(), FlowDown);
	flow(n.decl(), n, FlowUp);
    }

    public void visit(LocalDecl n) {
	if (n.init() != null) {
	    flow(n, n.init(), FlowDown);
	    flow(n.init(), n, FlowUp);
	}
	else {
	    leaf(n);
	}
    }

    public void visit(Loop n) {
	// TODO Auto-generated method stub

    }

    public void visit(MethodDecl n) {
	embedded(n);
    }

    public void visit(New n) {
	// TODO Auto-generated method stub

    }

    public void visit(NewArray n) {
	assert false : "unimplemented visit " + n;
    }

    public void visit(Node n) {

    }

    public void visit(NullLit n) {
	leaf(n);
    }

    public void visit(NumLit n) {

    }

    public void visit(PackageNode n) {
	leaf(n);
    }

    Node returnTarget;

    Node returnTarget() {
	return returnTarget;
    }

    void returnTarget(Node n) {
	returnTarget = n;
    }

    public void visit(Return n) {
	if (n.expr() != null) {
	    flow(n, n.expr(), FlowDown);
	    flow(n.expr(), n, FlowUp);
	    flow(n, returnTarget(), Return);
	}
	else {
	    leaf(n);
	}
    }

    public void visit(SourceCollection n) {
	leaf(n);
    }

    public void visit(SourceFile n) {
	leaf(n);
    }

    public void visit(Special n) {
	if (n.qualifier() != null) {
	    flow(n, n.qualifier(), FlowDown);
	    flow(n.qualifier(), n, FlowUp);
	}
	else {
	    leaf(n);
	}
    }

    public void visit(Stmt n) {
	// TODO Auto-generated method stub

    }

    public void visit(StringLit n) {
	leaf(n);
    }

    public void visit(Switch n) {
	Node oldBreakTarget = breakTarget(null);
	breakTarget(null, n);

	flow(n, n.expr(), FlowDown);
	Node prev = null;
	for (SwitchElement s : n.elements()) {
	    if (prev != null)
		flow(prev, s, FlowNext);
	    prev = s;
	    if (s instanceof Case) {
		Case c = (Case) s;
		flow(n.expr(), c, CaseSplit);
	    }
	}
	if (prev == null)
	    prev = n.expr();
	flow(prev, n, FlowUp);

	breakTarget(null, oldBreakTarget);
    }

    public void visit(SwitchBlock n) {
	Node prev = visitList(n, FlowDown, n.statements());
	if (prev != n) {
	    flow(prev, n, FlowUp);
	}
    }

    public void visit(Synchronized n) {
	flow(n, n.expr(), FlowDown);
	flow(n.expr(), n.body(), FlowNext);
	flow(n.body(), n, FlowUp);
    }

    public void visit(Term n) {
	// TODO Auto-generated method stub
    }

    void exceptionalFlows(Node n, Type exceptionType, Context context) {
	for (int i = tries.size() - 1; i >= 0; i--) {
	    Try trie = tries.get(i);
	    boolean caught = false;
	    for (Catch cb : trie.catchBlocks()) {
		if (ts.isSubtype(exceptionType, cb.catchType(), context)) {
		    // Definitely caught.
		    caught = true;
		    flow(n, cb, new ExceptionalFlow(exceptionType));
		    break;
		}
		if (ts.isSubtype(cb.catchType(), exceptionType, context)) {
		    // Possibly caught.
		    flow(n, cb, new ExceptionalFlow(exceptionType));
		}
	    }
	    if (!caught && trie.finallyBlock() != null) {
		// Flow through the finally block, then continue propagating the
		// exception outward.
		Flow ef = new ExceptionalFlow(exceptionType);
		flow(n, trie.finallyBlock(), new FinallyEnterFlow(n, ef));
		flow(trie.finallyBlock(), trie, new FinallyExitFlow(n, ef));
		n = trie.finallyBlock();
	    }
	    if (caught)
		break;
	}
    }

    public void visit(Throw n) {
	flow(n, n.expr(), FlowDown);
	flow(n.expr(), n, FlowUp);
	exceptionalFlows(n, n.expr().type(), ts.emptyContext());
    }

    List<Try> tries = new ArrayList<Try>();;

    public void visit(Try n) {
	tries.add(n);
	flow(n, n.tryBlock(), FlowDown);
	if (n.finallyBlock() != null) {
	    flow(n.tryBlock(), n.finallyBlock(), new FinallyEnterFlow(n.tryBlock(), FlowNext));
	    flow(n.finallyBlock(), n, new FinallyExitFlow(n.tryBlock(), FlowNext));
	    for (Catch cb : n.catchBlocks()) {
		flow(cb, cb.formal(), FlowDown);
		flow(cb.formal(), cb.body(), FlowNext);
		flow(cb.body(), cb, FlowUp);
		flow(cb, n.finallyBlock(), new FinallyEnterFlow(cb, FlowNext));
		flow(n.finallyBlock(), n, new FinallyExitFlow(cb, FlowNext));
	    }
	}
	else {
	    flow(n.tryBlock(), n, FlowUp);
	    for (Catch cb : n.catchBlocks()) {
		flow(cb, cb.formal(), FlowDown);
		flow(cb.formal(), cb.body(), FlowNext);
		flow(cb.body(), cb, FlowUp);
		flow(cb, n, FlowUp);
	    }
	}
	tries.remove(tries.size() - 1);
    }

    public void visit(TypeNode n) {
	leaf(n);
    }

    public void visit(Unary n) {
	flow(n, n.expr(), FlowDown);
	flow(n.expr(), n, FlowUp);
    }

    public void visit(While n) {
	Node oldBreak = breakTarget(null);
	Node oldContinue = continueTarget(null);
	breakTarget(null, n);
	continueTarget(null, n.cond());

	flow(n, n.cond(), FlowDown);
	flow(n.cond(), n.body(), True);
	flow(n.cond(), n, False);
	flow(n.body(), n.cond(), FlowNext);

	breakTarget(null, oldBreak);
	continueTarget(null, oldContinue);
    }

    public void embedded(Node n) {
	if (n instanceof CodeNode) {
	    CodeNode cn = (CodeNode) n;
	    if (cn.codeBody() != null) {
		Node r = returnTarget();
		returnTarget(n);
		inflows.put(cn, createInitialItem(cn, true));
		flow(cn, cn.codeBody(), FlowDown);
		flow(cn.codeBody(), cn, FlowUp);
		returnTarget(r);
	    }
	}
	leaf(n);
    }

}
