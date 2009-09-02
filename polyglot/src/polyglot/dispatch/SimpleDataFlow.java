package polyglot.dispatch;

import java.util.*;

import polyglot.ast.*;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.visit.CFGBuilder;
import polyglot.visit.FlowGraph;

/** This visitor adds a Ext node to the input node. */
public class SimpleDataFlow {
    CFGBuilder v;
    List succs;

    void flowDown(Node parent, Node child) {
	child.accept(this);
    }

    void flowTrue(Node parent, Node child) {
	flowNext(parent, child);
    }

    void flowFalse(Node parent, Node child) {
	flowNext(parent, child);
    }

    void flowNext(Node sister, Node brother) {
    }

    void flowUp(Node child, Node parent) {}

    void flowJoinTrue(Node child, Node parent) {
	flowUp(child, parent);
    }

    void flowJoinFalse(Node child, Node parent) {
	flowUp(child, parent);
    }

    void visit(ArrayAccess n) {
	ArrayAccess r = n;
	flowDown(r, r.array());
	flowNext(r.array(), r.index());
	flowUp(r.index(), r);
    }

    void visit(ArrayInit n) {
	ArrayInit r = n;
	flowDownListUp(r, r.elements());
    }

    void visit(Assert n) {
	Assert r = n;
	if (r.errorMessage() != null) {
	    flowDown(r, r.cond());
	    flowNext(r.cond(), r.errorMessage());
	    flowUp(r.errorMessage(), r);
	}
	else {
	    flowDown(r, r.cond());
	    flowUp(r.cond(), r);
	}
    }

    void visit(LocalAssign n) {
	LocalAssign r = n;
	if (r.operator() == Assign_c.ASSIGN) {
	    // do not visit left()
	    // l = e: visit e -> (l = e)
	    // v.visitCFG(local(), right(), ENTRY);

	    flowDown(r, r.right());
	    flowUp(r.right(), r);
	}
	else {
	    flowDown(r, r.local());
	    flowNext(r.local(), r.right());
	    flowUp(r.right(), r);
	}
    }

    void visit(FieldAssign n) {
	FieldAssign r = n;
	if (r.operator() == Assign_c.ASSIGN) {
	    // o.f = e: visit o -> e -> (o.f = e)
	    flowDown(r, r.target());
	    flowNext(r.target(), r.right());
	    flowUp(r.right(), r);
	}
	else {
	    flowDown(r, r.target());
	    //	    flowNext(r.target(), r.left(nf));
	    //	    flowNext(r.left(nf), r.right());
	    flowNext(r.target(), r.right());
	    flowUp(r.right(), r);
	}
    }

    void visit(ArrayAccessAssign n) {
	ArrayAccessAssign r = n;
	if (r.operator() == Assign_c.ASSIGN) {
	    // a[i] = e: visit a -> i -> e -> (a[i] = e)
	    flowDown(r, r.array());
	    flowNext(r.array(), r.index());
	    flowNext(r.index(), r.right());
	    flowUp(r.right(), r);
	}
	else {
	    flowDown(r, r.array());
	    flowNext(r.array(), r.index());
	    flowNext(r.index(), r.right());
	    flowUp(r.right(), r);
	}
    }

    void visit(Binary n) {
	Binary r = n;
	if (r.operator() == Binary_c.COND_AND || r.operator() == Binary_c.COND_OR) {
	    // short-circuit
	    if (r.left() instanceof BooleanLit) {
		BooleanLit b = (BooleanLit) r.left();
		if ((b.value() && r.operator() == Binary_c.COND_OR) || (!b.value() && r.operator() == Binary_c.COND_AND)) {
		    flowDown(r, r.left());
		    flowUp(r.left(), r);
		}
		else {
		    flowDown(r, r.left());
		    flowNext(r.left(), r.right());
		    flowUp(r.right(), r);
		}
	    }
	    else {
		if (r.operator() == Binary_c.COND_AND) {
		    // AND operator
		    // short circuit means that left is false
		    v.visitCFG(r.left(), FlowGraph.EDGE_KEY_TRUE, r.right(), Term.ENTRY, FlowGraph.EDGE_KEY_FALSE, r, Term.EXIT);
		}
		else {
		    // OR operator
		    // short circuit means that left is true
		    v.visitCFG(r.left(), FlowGraph.EDGE_KEY_FALSE, r.right(), Term.ENTRY, FlowGraph.EDGE_KEY_TRUE, r, Term.EXIT);
		}
		v.visitCFG(r.right(), FlowGraph.EDGE_KEY_TRUE, r, Term.EXIT, FlowGraph.EDGE_KEY_FALSE, r, Term.EXIT);
	    }
	}
	else {
	    if (r.left().type().isBoolean() && r.right().type().isBoolean()) {
		v.visitCFG(r.left(), FlowGraph.EDGE_KEY_TRUE, r.right(), Term.ENTRY, FlowGraph.EDGE_KEY_FALSE, r.right(), Term.ENTRY);
		v.visitCFG(r.right(), FlowGraph.EDGE_KEY_TRUE, r, Term.EXIT, FlowGraph.EDGE_KEY_FALSE, r, Term.EXIT);
	    }
	    else {
		flowDown(r, r.left());
		flowNext(r.left(), r.right());
		flowUp(r.right(), r);
	    }
	}
    }

    void flowDownListUp(Node parent, List<? extends Node> children) {
	Node prev = parent;
	for (Node child : children) {
	    if (prev == parent)
		flowDown(parent, child);
	    else
		flowNext(prev, child);
	    prev = child;
	}
	if (prev != parent)
	    flowUp(prev, parent);
    }

    void visit(Block n) {
	Block r = n;
	flowDownListUp(r, r.statements());
    }

    void visit(BooleanLit n) {
	BooleanLit r = n;
    }

    void visit(Branch n) {
	v.visitBranchTarget(((Branch_c) n));
    }

    void visit(CanonicalTypeNode n) {
	CanonicalTypeNode r = n;
    }

    void visit(Case n) {
	Case r = n;
	if (r.expr() != null) {
	    flowDown(r, r.expr());
	    flowUp(r.expr(), r);
	}
    }

    void visit(Cast n) {
	Cast r = n;
	flowDown(r, r.castType());
	flowNext(r.castType(), r.expr());
	flowUp(r.expr(), r);
    }

    void visit(Catch n) {
	Catch r = n;
	flowDown(r, r.formal());
	flowNext(r.formal(), r.body());
	flowUp(r.body(), r);
    }

    void visit(CharLit n) {
	CharLit r = n;
    }

    void visit(ClassBody n) {
	ClassBody r = n;
    }

    void visit(ClassDecl n) {
	ClassDecl r = n;
	flowDown(r, r.body());
	flowUp(r.body(), r);
    }

    void visit(ClassLit n) {
	ClassLit r = n;
	flowDown(r, r.typeNode());
	flowUp(r.typeNode(), r);
    }

    void visit(Conditional n) {
	Conditional r = n;
	flowDown(r, r.cond());
	flowTrue(r.cond(), r.consequent());
	flowJoinTrue(r.consequent(), r);
	flowFalse(r.cond(), r.alternative());
	flowJoinFalse(r.alternative(), r);
    }

    void flowNextListUp(Node brother, List<? extends Node> sisters, Node parent) {
	Node prev = brother;
	for (Node sister : sisters) {
	    flowNext(prev, sister);
	    prev = sister;
	}
	flowUp(prev, parent);
    }

    void flowNextListNext(Node brother, List<? extends Node> sisters, Node prodigal) {
	Node prev = brother;
	for (Node sister : sisters) {
	    flowNext(prev, sister);
	    prev = sister;
	}
	flowNext(prev, prodigal);
    }
    void flowDownListNext(Node parent, List<? extends Node> sisters, Node prodigal) {
	Node prev = parent;
	for (Node sister : sisters) {
	    if (prev == parent)
		flowDown(prev, sister);
	    else
		flowNext(prev, sister);
	    prev = sister;
	}
	if (prev == parent)
	    flowDown(prev, prodigal);
	else
	    flowNext(prev, prodigal);
    }

    void visit(ConstructorCall n) {
	ConstructorCall r = n;
	if (r.qualifier() != null) {
	    flowDown(r, r.qualifier());
	    flowNextListUp(r.qualifier(), r.arguments(), r);
	}
	else {
	    flowDownListUp(r, r.arguments());
	}
    }

    void visit(ConstructorDecl n) {
	ConstructorDecl r = n;
	if (r.body() != null) {
	    flowDownListNext(r, r.formals(), r.body());
	    flowUp(r.body(), r);
	}
	else {
	    flowDownListUp(r, r.formals());
	}
    }

    void visit(Do n) {
	Do r = n;
	v.push(r).visitCFG(r.body(), r.cond(), Term.ENTRY);

	if (r.condIsConstantTrue()) {
	    flowNext(r.cond(), r.body());
	}
	else {
	    v.visitCFG(r.cond(), FlowGraph.EDGE_KEY_TRUE, r.body(), Term.ENTRY, FlowGraph.EDGE_KEY_FALSE, r, Term.EXIT);
	}
    }

    void visit(Empty n) {
	Empty r = n;
    }

    void visit(Eval n) {
	Eval r = n;
	Node child = r.expr();
	flowDown(r, child);
	flowUp(child, r);
    }

    void visit(Field n) {
	Field r = n;
	if (r.target() instanceof Term) {
	    v.visitCFG((Term) r.target(), r, Term.EXIT);
	}
    }

    void visit(FieldDecl n) {
	FieldDecl r = n;
	if (r.init() != null) {
	    flowNext(r.type(), r.init());
	    Node child = r.init();
	    flowDown(r, child);
	    flowUp(child, r);
	}
	else {
	    Node child = r.type();
	    flowDown(r, child);
	    flowUp(child, r);
	}
    }

    void visit(FloatLit n) {
	FloatLit r = n;
    }

    void visit(For n) {
	For r = n;
	v.visitCFGList(r.inits(), r.cond() != null ? (Term) r.cond() : r.body(), Term.ENTRY);

	if (r.cond() != null) {
	    if (r.condIsConstantTrue()) {
		flowNext(r.cond(), r.body());
	    }
	    else {
		v.visitCFG(r.cond(), FlowGraph.EDGE_KEY_TRUE, r.body(), Term.ENTRY, FlowGraph.EDGE_KEY_FALSE, r, Term.EXIT);
	    }
	}

	v.push(r).visitCFG(r.body(), r.continueTarget(), Term.ENTRY);
	v.visitCFGList(r.iters(), r.cond() != null ? (Term) r.cond() : r.body(), Term.ENTRY);
    }

    void visit(Formal n) {
	Formal r = n;
	Node child = r.type();
	flowDown(r, child);
	flowUp(child, r);
    }

    void visit(If n) {
	If r = n;
	if (r.alternative() == null) {
	    v.visitCFG(r.cond(), FlowGraph.EDGE_KEY_TRUE, r.consequent(), Term.ENTRY, FlowGraph.EDGE_KEY_FALSE, r, Term.EXIT);
	    Node child = r.consequent();
	    flowDown(r, child);
	    flowUp(child, r);
	}
	else {
	    v.visitCFG(r.cond(), FlowGraph.EDGE_KEY_TRUE, r.consequent(), Term.ENTRY, FlowGraph.EDGE_KEY_FALSE, r.alternative(), Term.ENTRY);
	    Node child = r.consequent();
	    flowDown(r, child);
	    flowUp(child, r);
	    Node child1 = r.alternative();
	    flowDown(r, child1);
	    flowUp(child1, r);
	}
    }

    void visit(Initializer n) {
	Initializer r = n;
	Node child = r.body();
	flowDown(r, child);
	flowUp(child, r);
    }

    void visit(Instanceof n) {
	Instanceof r = n;
	flowNext(r.expr(), r.compareType());
	Node child = r.compareType();
	flowDown(r, child);
	flowUp(child, r);
    }

    void visit(IntLit n) {
	IntLit r = n;
    }

    void visit(Labeled n) {
	Labeled r = n;
	v.push(r).visitCFG(r.statement(), r, Term.EXIT);
    }

    void visit(Lit n) {
	Lit r = n;
    }

    void visit(Local n) {
	Local r = n;
    }

    void visit(LocalClassDecl n) {
	LocalClassDecl r = n;
	Node child = r.decl();
	flowDown(r, child);
	flowUp(child, r);
    }

    void visit(LocalDecl n) {
	LocalDecl r = n;
	if (r.init() != null) {
	    flowNext(r.type(), r.init());
	    Node child = r.init();
	    flowDown(r, child);
	    flowUp(child, r);
	}
	else {
	    Node child = r.type();
	    flowDown(r, child);
	    flowUp(child, r);
	}
    }

    void visit(MethodDecl n) {
	MethodDecl r = n;
	v.visitCFGList(r.formals(), r.returnType(), Term.ENTRY);

	if (r.body() == null) {
	    Node child = r.returnType();
	    flowDown(r, child);
	    flowUp(child, r);
	}
	else {
	    flowNext(r.returnType(), r.body());
	    Node child = r.body();
	    flowDown(r, child);
	    flowUp(child, r);
	}
    }

    void visit(NewArray n) {
	NewArray r = n;
	if (r.init() != null) {
	    v.visitCFG(r.baseType(), NewArray_c.listChild(r.dims(), r.init()), Term.ENTRY);
	    v.visitCFGList(r.dims(), r.init(), Term.ENTRY);
	    Node child = r.init();
	    flowDown(r, child);
	    flowUp(child, r);
	}
	else {
	    v.visitCFG(r.baseType(), NewArray_c.listChild(r.dims(), null), Term.ENTRY);
	    flowDownListUp(r, r.dims());
	}
    }

    void visit(New n) {
	New r = n;
	if (r.qualifier() != null) {
	    flowNext(r.qualifier(), r.objectType());
	}

	if (r.body() != null) {
	    v.visitCFG(r.objectType(), New_c.listChild(r.arguments(), r.body()), Term.ENTRY);
	    v.visitCFGList(r.arguments(), r.body(), Term.ENTRY);
	    Node child = r.body();
	    flowDown(r, child);
	    flowUp(child, r);
	}
	else {
	    if (!r.arguments().isEmpty()) {
		v.visitCFG(r.objectType(), New_c.listChild(r.arguments(), null), Term.ENTRY);
		flowDownListUp(r, r.arguments());
	    }
	    else {
		Node child = r.objectType();
		flowDown(r, child);
		flowUp(child, r);
	    }
	}
    }

    void visit(NullLit n) {
	NullLit r = n;
    }

    void visit(NumLit n) {
	NumLit r = n;
    }

    void visit(Return n) {
	Return r = n;
	if (r.expr() != null) {
	    Node child = r.expr();
	    flowDown(r, child);
	    flowUp(child, r);
	}

	v.visitReturn(r);
    }

    void visit(Special n) {
	Special r = n;
	if (r.qualifier() != null) {
	    Node child = r.qualifier();
	    flowDown(r, child);
	    flowUp(child, r);
	}
    }

    void visit(StringLit n) {
	StringLit r = n;
    }

    void visit(SwitchBlock n) {
	SwitchBlock r = n;
	flowDownListUp(r, r.statements());
    }

    void visit(Switch n) {
	Switch r = n;
	SwitchElement prev = null;

	List<Term> cases = new ArrayList<Term>(r.elements().size() + 1);
	List<Integer> entry = new ArrayList<Integer>(r.elements().size() + 1);
	boolean hasDefault = false;

	for (Iterator<SwitchElement> i = r.elements().iterator(); i.hasNext();) {
	    SwitchElement s = (SwitchElement) i.next();

	    if (s instanceof Case) {
		cases.add(s);
		entry.add(Integer.valueOf(Term.ENTRY));

		if (((Case) s).expr() == null) {
		    hasDefault = true;
		}
	    }
	}

	// If there is no default case, add an edge to the end of the switch.
	if (!hasDefault) {
	    cases.add(r);
	    entry.add(Term.EXIT);
	}

	v.visitCFG(r.expr(), FlowGraph.EDGE_KEY_OTHER, cases, entry);
	v.push(r).visitCFGList(r.elements(), r, Term.EXIT);
    }

    void visit(Synchronized n) {
	Synchronized r = n;
	flowNext(r.expr(), r.body());
	Node child = r.body();
	flowDown(r, child);
	flowUp(child, r);
    }

    void visit(Throw n) {
	Throw r = n;
	Node child = r.expr();
	flowDown(r, child);
	flowUp(child, r);
    }

    void visit(Try n) {
	Try r = n;
	// Add edges from the try entry to any catch blocks for Error and
	// RuntimeException.
	TypeSystem ts = v.typeSystem();

	CFGBuilder v1 = v.push(r, false);
	CFGBuilder v2 = v.push(r, true);

	for (Iterator i = ts.uncheckedExceptions().iterator(); i.hasNext();) {
	    Type type = (Type) i.next();
	    v1.visitThrow(r.tryBlock(), Term.ENTRY, type);
	}

	// Handle the normal return case. The throw case will be handled
	// specially.
	if (r.finallyBlock() != null) {
	    v1.visitCFG(r.tryBlock(), r.finallyBlock(), Term.ENTRY);
	    Node child = r.finallyBlock();
	    flowDown(r, child);
	    flowUp(child, r);
	}
	else {
	    v1.visitCFG(r.tryBlock(), r, Term.EXIT);
	}

	for (Iterator it = r.catchBlocks().iterator(); it.hasNext();) {
	    Catch cb = (Catch) it.next();
	    if (r.finallyBlock() != null) {
		v2.visitCFG(cb, r.finallyBlock(), Term.ENTRY);
	    }
	    else {
		v2.visitCFG(cb, r, Term.EXIT);
	    }
	}
    }

    void visit(TypeNode n) {
	TypeNode r = n;
    }

    void visit(Unary n) {
	Unary r = n;
	if (r.expr().type().isBoolean()) {
	    v.visitCFG(r.expr(), FlowGraph.EDGE_KEY_TRUE, r, Term.EXIT, FlowGraph.EDGE_KEY_FALSE, r, Term.EXIT);
	}
	else {
	    Node child = r.expr();
	    flowDown(r, child);
	    flowUp(child, r);
	}
    }

    void visit(While n) {
	While r = n;
	if (r.condIsConstantTrue()) {
	    flowNext(r.cond(), r.body());
	}
	else {
	    v.visitCFG(r.cond(), FlowGraph.EDGE_KEY_TRUE, r.body(), Term.ENTRY, FlowGraph.EDGE_KEY_FALSE, r, Term.EXIT);
	}

	v.push(r).visitCFG(r.body(), r.cond(), Term.ENTRY);
    }
}
