package polyglot.ast;

import polyglot.types.LazyRef;
import polyglot.types.UnknownType;
import polyglot.visit.TypeChecker;

public class TypeCheckTypeGoal extends TypeCheckFragmentGoal {
	public TypeCheckTypeGoal(Node parent, Node n, TypeChecker v, LazyRef r,
			boolean mightFail) {
		super(parent, n, v, r, mightFail);
	}

	@Override
	public boolean runTask() {
		boolean result = super.runTask();
		if (result) {
			if (r.getCached() instanceof UnknownType) {
				assert false;
			}
		}
		return result;
	}
}
