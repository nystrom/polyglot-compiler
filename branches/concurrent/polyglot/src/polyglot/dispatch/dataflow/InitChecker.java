package polyglot.dispatch.dataflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import polyglot.ast.Assign;
import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassMember;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldDecl;
import polyglot.ast.Initializer;
import polyglot.ast.Local;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.dispatch.dataflow.InitChecker.DefiniteAssignmentFlowItem.Count;
import polyglot.frontend.Globals;
import polyglot.types.FieldDef;
import polyglot.types.LocalDef;
import polyglot.util.ErrorInfo;
import polyglot.visit.NodeVisitor;

public class InitChecker extends NodeVisitor {

    public static class DefiniteAssignmentFlowItem implements FlowItem<DefiniteAssignmentFlowItem> {
	enum Count {
	    ZERO, ONE, MANY;
	}

	Map<FieldDef, Count> fields;
	Map<LocalDef, Count> locals;
	boolean unreachable;

	@Override
	public String toString() {
	    if (unreachable)
		return "UNREACHABLE";
	    return "l=" + (locals != null ? locals.toString() : "{}") + " f=" + (fields != null ? fields.toString() : "{}");
	}

	public DefiniteAssignmentFlowItem mergeWith(DefiniteAssignmentFlowItem that) {
	    if (this == that)
		return this;
	    
	    if (this.unreachable)
		return that;
	    if (that.unreachable)
		return this; 

	    Map<FieldDef, Count> thisFields = this.fields == null ? Collections.<FieldDef, Count> emptyMap() : this.fields;
	    Map<LocalDef, Count> thisLocals = this.locals == null ? Collections.<LocalDef, Count> emptyMap() : this.locals;
	    Map<FieldDef, Count> thatFields = that.fields == null ? Collections.<FieldDef, Count> emptyMap() : that.fields;
	    Map<LocalDef, Count> thatLocals = that.locals == null ? Collections.<LocalDef, Count> emptyMap() : that.locals;

	    Map<FieldDef, Count> newFields = mergeMaps(thisFields, thatFields);
	    Map<LocalDef, Count> newLocals = mergeMaps(thisLocals, thatLocals);

	    DefiniteAssignmentFlowItem i = new DefiniteAssignmentFlowItem();
	    i.fields = newFields;
	    i.locals = newLocals;
	    return i;
	}

	private <K> Map<K, Count> mergeMaps(Map<K, Count> thisFields, Map<K, Count> thatFields) {
	    Map<K, Count> newFields = new HashMap<K, Count>();
	    for (Map.Entry<K, Count> e : thisFields.entrySet()) {
		Count c = thatFields.get(e.getKey());
		if (c == null) // null is bottom
		    newFields.put(e.getKey(), Count.ZERO);
		else {
		    if (c == Count.ZERO || e.getValue() == Count.ZERO) {
			newFields.put(e.getKey(), Count.ZERO);
		    }
		    else if (c == Count.MANY && e.getValue() == Count.MANY) {
			newFields.put(e.getKey(), Count.MANY);
		    }
		    else {
			newFields.put(e.getKey(), Count.ONE);
		    }
		}
	    }
	    for (Map.Entry<K, Count> e : thatFields.entrySet()) {
		Count c = thisFields.get(e.getKey());
		if (c == null) // null is bottom
		    newFields.put(e.getKey(), Count.ZERO);
	    }
	    //    	System.out.println("merge " + thisFields + " and " + thatFields + " = " + newFields);
	    return newFields;
	}

	public final boolean equals(Object o) {
	    if (o instanceof DefiniteAssignmentFlowItem) {
		return equals((DefiniteAssignmentFlowItem) o);
	    }
	    return false;
	}

	public boolean equals(DefiniteAssignmentFlowItem that) {
	    if (this == that)
		return true;
	    if (this.unreachable != that.unreachable)
		return false;
	    if (this.unreachable)
		return true;
	    if (fields == null && that.fields != null && that.fields.size() > 0)
		return false;
	    if (locals == null && that.locals != null && that.locals.size() > 0)
		return false;
	    if (that.fields == null && fields != null && fields.size() > 0)
		return false;
	    if (that.locals == null && locals != null && locals.size() > 0)
		return false;

	    return equalMaps(fields, that.fields) && equalMaps(locals, that.locals);
	}

	private <K> boolean equalMaps(Map<K, Count> m1, Map<K, Count> m2) {
	    if (m1 != null && m2 != null) {
		if (m1.size() != m2.size())
		    return false;
		for (Map.Entry<K, Count> e : m1.entrySet()) {
		    Count c = m2.get(e.getKey());
		    if (c == null)
			return false;
		    if (c != e.getValue())
			return false;
		}
		return true;
	    }
	    else {
		return m1 == null && m2 == null;
	    }
	}
    }

    public class DefiniteAssignment extends DataFlow<DefiniteAssignmentFlowItem> {
	private Map<FieldDef, Count> initFields;

	public DefiniteAssignment(Map<FieldDef,Count> initFields) {
	    this.initFields = initFields;
	}

	@Override
	protected DefiniteAssignmentFlowItem entryItem() {
	    DefiniteAssignmentFlowItem i = new DefiniteAssignmentFlowItem();
	    i.fields = initFields;
	    return i;
	}

	@Override
	protected DefiniteAssignmentFlowItem initialItem() {
	    return unreachableItem();
	}
	
	DefiniteAssignmentFlowItem unreachable;
	
	@Override
	protected DefiniteAssignmentFlowItem unreachableItem() {
	    if (unreachable == null) {
		unreachable = new DefiniteAssignmentFlowItem();
		unreachable.unreachable = true;
	    }
	    return unreachable;
	}

	protected DefiniteAssignmentFlowItem def(LocalDef def, DefiniteAssignmentFlowItem item) {
//	    if (item.unreachable) return item;
	    DefiniteAssignmentFlowItem neu = new DefiniteAssignmentFlowItem();
	    neu.fields = item.fields != null ? new HashMap<FieldDef, Count>(item.fields) : null;
	    neu.locals = new HashMap<LocalDef, Count>(item.locals != null ? item.locals : Collections.<LocalDef,Count>emptyMap());
	    Count c = neu.locals.get(def);
	    if (c == null) c = Count.ZERO;
	    switch (c) {
	    case ZERO:
		neu.locals.put(def, Count.ONE);
		break;
	    default:
		neu.locals.put(def, Count.MANY);
		break;
	    }
	    return neu;
	}

	public DefiniteAssignmentFlowItem visit(polyglot.ast.LocalDecl_c n, Node parent, DefiniteAssignmentFlowItem item) {
	    if (n.init() != null)
		return def(n.localDef(), item);
	    return item;
	}

	public DefiniteAssignmentFlowItem visit(polyglot.ast.Formal_c n, Node parent, DefiniteAssignmentFlowItem item) {
	    return def(n.localDef(), item);
	}

	@Override
	protected DefiniteAssignmentFlowItem localAssign(Local left, Expr right, DefiniteAssignmentFlowItem item) {
	    //	    System.out.println("assign " + left + " = " + right);
	    return def(left.localInstance().def(), item);
	}

	Map<Node, DefiniteAssignmentFlowItem> map = new HashMap<Node, DefiniteAssignmentFlowItem>();
	Set<Node> lhs = new HashSet<Node>();

	@Override
	public DefiniteAssignmentFlowItem getItem(Node n) {
	    return map.get(n);
	}

	@Override
	public void putItem(Node n, DefiniteAssignmentFlowItem i) {
	    if (n instanceof Local)
		map.put(n, i);
	    if (n instanceof Assign && ((Assign) n).left() instanceof Local) {
		map.put(n, i);
		lhs.add(((Assign) n).left());
	    }
	}

	public Count get(Node n, LocalDef v) {
	    DefiniteAssignmentFlowItem item = getItem(n);
	    if (item == null)
		return Count.ZERO;
	    if (item.unreachable)
		return Count.ZERO;
	    if (item.locals == null)
		return Count.ZERO;
	    Count c = item.locals.get(v);
	    if (c == null)
		return Count.ZERO;
	    return c;
	}

	public Count get(Node n, FieldDef v) {
	    DefiniteAssignmentFlowItem item = getItem(n);
	    if (item == null)
		return Count.ZERO;
	    if (item.unreachable)
		return Count.ZERO;
	    if (item.fields == null)
		return Count.ZERO;
	    Count c = item.fields.get(v);
	    if (c == null)
		return Count.ZERO;
	    return c;
	}
    };


    void checkLocals(Node child, Node parent) {
	final DefiniteAssignment ff = new DefiniteAssignment(null);
	child.accept(ff, parent, ff.entryItem());

	for (Node lhs : ff.lhs) {
	    ff.map.remove(lhs);
	}

	for (Entry<Node, DefiniteAssignmentFlowItem> e : ff.map.entrySet()) {
	    if (e.getKey() instanceof Assign) {
		Assign a = (Assign) e.getKey();
		Local l = (Local) a.left();
		DefiniteAssignmentFlowItem item = e.getValue();
		Count c;
		if (item.locals == null)
		    c = null;
		else
		    c = item.locals.get(l.localInstance().def());
		if (c == null) c = Count.ZERO;
		if (c == Count.MANY && l.localInstance().def().flags().isFinal()) {
		    Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Final local variable defined more than once.", l.position());
		}
	    }
	}

	for (Entry<Node, DefiniteAssignmentFlowItem> e : ff.map.entrySet()) {
	    if (e.getKey() instanceof Local) {
		Local l = (Local) e.getKey();
		DefiniteAssignmentFlowItem item = e.getValue();

				if (child.context().isLocal(l.name().id())) {
		Count c;
		if (item.locals == null)
		    c = null;
		else
		    c = item.locals.get(l.localInstance().def());
					if (c == null)
						c = Count.ZERO;
		if (c == Count.ZERO) {
						Globals.Compiler()
								.errorQueue()
								.enqueue(
										ErrorInfo.SEMANTIC_ERROR,
										"Local variable "
												+ l.name()
												+ " may not have been initialized.",
										l.position());
					}
		}
	    }
	}
    }

    void checkFields(Node child, Node parent, Map<FieldDef, Count> fields) {
	final DefiniteAssignment ff = new DefiniteAssignment(fields);
	child.accept(ff, parent, ff.entryItem());

	for (Node lhs : ff.lhs) {
	    ff.map.remove(lhs);
	}

	for (Entry<Node, DefiniteAssignmentFlowItem> e : ff.map.entrySet()) {
	    if (e.getKey() instanceof Field) {
		Field f = (Field) e.getKey();
		DefiniteAssignmentFlowItem item = e.getValue();
		Count c;
		if (item.fields == null)
		    c = null;
		else
		    c = item.fields.get(f.fieldInstance().def());
		if (c == null) c = Count.ZERO;
		if (c == Count.MANY && f.fieldInstance().def().flags().isFinal()) {
		    Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Final field defined more than once.", f.position());
		}
	    }
	}
    }

    @Override
    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
	if (n instanceof ClassBody) {
	    ClassBody d = (ClassBody) n;

	    Map<FieldDef,Count> initializedFields = new HashMap<FieldDef,Count>();

	    for (ClassMember m : d.members()) {
		if (m instanceof FieldDecl) {
		    FieldDecl fd = (FieldDecl) m;
		    if (fd.init() != null) {
			initializedFields.put(fd.fieldDef(), Count.ONE);
		    }
		}
	    }

	    for (ClassMember m : d.members()) {
		if (m instanceof MethodDecl) {
		    checkLocals(m, d);
		}
		if (m instanceof FieldDecl) {
		    checkLocals(m, d);
		}
		if (m instanceof Initializer) {
		    checkLocals(m, d);
		    checkFields(m, d, initializedFields);
		}
		if (m instanceof ConstructorDecl) {
		    checkLocals(m, d);

		    Block body = ((ConstructorDecl) m).body();

		    if (body != null) {
			Stmt s = body.statements().isEmpty() ? null : body.statements().get(0);
			if (s == null || s instanceof ConstructorCall && ((ConstructorCall) s).kind() == ConstructorCall.SUPER)
			    checkFields(m, d, initializedFields);
		    }
		}
	    }

	}
	return n;
    }

}
