package polyglot.ast;

import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.ErrorInfo;

public class TypeCheckTypeGoal extends TypeCheckFragmentGoal {
	public TypeCheckTypeGoal(Node n, Job job, TypeSystem ts, NodeFactory nf, LazyRef r, boolean mightFail) {
		super(n, job, ts, nf, r, mightFail);
	}

	@Override
	public boolean runTask() {
		boolean result = super.runTask();
		if (result) {
			if (r.getCached() instanceof UnknownType) {
//			    assert false;
			        Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Could not compute type.", n.position());
			        return false;
			}
		}
		return result;
	}
}
