/**
 * 
 */
package polyglot.ast;

import java.util.Collections;
import java.util.List;

import polyglot.frontend.*;
import polyglot.types.*;
import polyglot.util.CollectionUtil;
import polyglot.visit.TypeChecker;

public class TypeCheckFragmentGoal extends AbstractGoal_c {
	Node parent;
	Node n;
	TypeChecker v;
	LazyRef r;

	public TypeCheckFragmentGoal(Node parent, Node n, TypeChecker v, LazyRef r) {
		this.parent = parent;
		this.n = n;
		this.v = v;
		this.r = r;
	}
	
	public List<Goal> prereqs() {
		List<Goal> l = super.prereqs();
		List<Goal> l2 = Collections.singletonList(v.job().extensionInfo().scheduler().PreTypeCheck(v.job()));
		if (l.isEmpty())
			return l2;
		else
			return CollectionUtil.<Goal>append(l, l2);
	}

	public boolean run() {
		Goal g = v.job().extensionInfo().scheduler().PreTypeCheck(v.job());
		assert g.hasBeenReached();
		if (state() == Goal.Status.RUNNING_RECURSIVE) {
			r.update(r.getCached());
			return false;
		}

		try {
			Node m = parent.visitChild(n, v);
			v.job().nodeMemo().put(n, m);
			return r.known();
		}
		catch (SchedulerException e) {
			return false;
		}
	}
}