/**
 * 
 */
package polyglot.ast;

import java.util.Collections;
import java.util.List;

import polyglot.dispatch.TypeChecker;
import polyglot.frontend.*;
import polyglot.types.LazyRef;
import polyglot.types.TypeSystem;
import polyglot.util.CollectionUtil;

public class TypeCheckFragmentGoal extends AbstractGoal_c {
    protected Node n;
    protected LazyRef r;
    protected boolean mightFail;
    private Job job;
    private TypeSystem ts;
    private NodeFactory nf;

    public TypeCheckFragmentGoal(Node n, Job job, TypeSystem ts, NodeFactory nf, LazyRef r, boolean mightFail) {
	this.n = n;
	this.r = r;
	this.job = job;
	this.ts = ts;
	this.nf = nf;
	this.mightFail = mightFail;
    }
    
    public List<Goal> prereqs() {
	List<Goal> l = super.prereqs();
	List<Goal> l2 = Collections.singletonList(job.extensionInfo().scheduler().PreTypeCheck(job));
	if (l.isEmpty())
	    return l2;
	else
	    return CollectionUtil.<Goal> append(l, l2);
    }

    public boolean runTask() {
	Goal g = job.extensionInfo().scheduler().PreTypeCheck(job);
	assert g.hasBeenReached();

	if (state() == Goal.Status.RUNNING_RECURSIVE) {
	    r.update(r.getCached()); // marks r known
	    // if (! mightFail)
	    // v.job().compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
	    // "Recursive resolution for " + n + ".", n.position());
	    return mightFail;
	}

	try {
	    TypeChecker d = new TypeChecker(job, ts, nf);
	    Node m = d.visit(n);
//	    Node m = parent.visitChild(n, v);
	    return mightFail || r.known();
	}
	catch (SchedulerException e) {
	    return false;
	}
    }
    
    public String toString() {
	return job + ":" + job.extensionInfo() + ":" + name() + " (" + stateString() + ") " + n;
    }
}