package polyglot.dispatch.dataflow;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import polyglot.ast.Assign;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Initializer;
import polyglot.ast.Local;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.dispatch.dataflow.InitChecker.DefiniteAssignmentFlowItem;
import polyglot.dispatch.dataflow.InitChecker.DefiniteAssignmentFlowItem.Count;
import polyglot.frontend.Globals;
import polyglot.types.FieldDef;
import polyglot.types.LocalDef;
import polyglot.util.ErrorInfo;
import polyglot.visit.NodeVisitor;

public class ExitChecker extends NodeVisitor {
	public class DefiniteAssignment extends
			DataFlow<DefiniteAssignmentFlowItem> {
		public DefiniteAssignment() {
		}

		@Override
		protected DefiniteAssignmentFlowItem entryItem() {
			DefiniteAssignmentFlowItem i = new DefiniteAssignmentFlowItem();
			return i;
		}

		@Override
		protected DefiniteAssignmentFlowItem unreachableItem() {
			return new DefiniteAssignmentFlowItem();
		}

		@Override
		protected DefiniteAssignmentFlowItem initialItem() {
			return unreachableItem();
		}

		protected DefiniteAssignmentFlowItem def(LocalDef def,
				DefiniteAssignmentFlowItem item) {
			DefiniteAssignmentFlowItem neu = new DefiniteAssignmentFlowItem();
			neu.fields = item.fields != null ? new HashMap<FieldDef, Count>(
					item.fields) : null;
			neu.locals = new HashMap<LocalDef, Count>(
					item.locals != null ? item.locals
							: Collections.<LocalDef, Count> emptyMap());
			Count c = neu.locals.get(def);
			if (c == null)
				c = Count.ZERO;
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

		public DefiniteAssignmentFlowItem visit(polyglot.ast.LocalDecl_c n,
				Node parent, DefiniteAssignmentFlowItem item) {
			if (n.init() != null)
				return def(n.localDef(), item);
			return item;
		}

		public DefiniteAssignmentFlowItem visit(polyglot.ast.Formal_c n,
				Node parent, DefiniteAssignmentFlowItem item) {
			return def(n.localDef(), item);
		}

		@Override
		protected DefiniteAssignmentFlowItem localAssign(Local left,
				Expr right, DefiniteAssignmentFlowItem item) {
			// System.out.println("assign " + left + " = " + right);
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
			Count c = item.locals.get(v);
			if (c == null)
				return Count.ZERO;
			return c;
		}

		public Count get(Node n, FieldDef v) {
			DefiniteAssignmentFlowItem item = getItem(n);
			if (item == null)
				return Count.ZERO;
			Count c = item.fields.get(v);
			if (c == null)
				return Count.ZERO;
			return c;
		}
	};

	void checkLocals(Node child, Node parent) {
		final DefiniteAssignment ff = new DefiniteAssignment();
		DefiniteAssignmentFlowItem i = child.accept(ff, parent, ff.entryItem());

		for (Entry<Node, DefiniteAssignmentFlowItem> e : ff.map.entrySet()) {
			if (e.getKey() instanceof Local) {
				Local l = (Local) e.getKey();
				DefiniteAssignmentFlowItem item = e.getValue();
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
									"Local variable " + l.name()
											+ " may not have been initialized.",
									l.position());
				}
			}
		}
	}

	@Override
	public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
		Node m = n;
		Node d = parent;
		if (m instanceof MethodDecl) {
			checkLocals(m, d);
		}
		if (m instanceof FieldDecl) {
			checkLocals(m, d);
		}
		if (m instanceof Initializer) {
			checkLocals(m, d);
		}
		if (m instanceof ConstructorDecl) {
			checkLocals(m, d);
		}

		return n;
	}

}
