package polyglot.visit;

import java.util.HashMap;
import java.util.Map;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;

public class ContextCache {
    protected Map<Integer, Context> cache;
    protected Job job;
    protected TypeSystem ts;
    protected NodeFactory nf;
    public NewParentCache pc;

    public ContextCache(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
	this.cache = new HashMap<Integer, Context>();
	this.pc = new NewParentCache(job, ts, nf);
    }

    public void invalidate() {
	cache = null;
    }

    public Context get(Node n) {
	if (cache != null) {
	    Context c = cache.get(n.nodeId());
	    if (c != null)
		return c;
	    
	    recomputeParent(n);
	}
	else {
	    recompute();
	}
	
	Context c = cache.get(n.nodeId());
	if (c != null)
	    return c;

	throw new InternalCompilerError("Could not find context for " + n);
    }
    
    
    protected void recomputeParent(Node n) {
	Context c = cache.get(n.nodeId());
	if (c != null) {
	    add(n, c);
	    return;
	}
	
	Integer p = pc.get(n);

	if (p == null) {
	    recompute();
	    return;
	}
	
	Node pn = job.findById(p);
	
	if (pn == null) {
	    recompute();
	    return;
	}
	
	recomputeParent(pn);
    }

    private void recompute() {
	recompute(job.ast(), ts.emptyContext());
    }

    protected void recompute(Node n, Context c) {
	cache = new HashMap<Integer, Context>();
	add(n, c);
    }

    public void add(Node n, Context c) {
	if (n == null)
	    return;
	ContextVisitor v = new ContextVisitor(job, ts, nf) {
	    @Override
	    protected Node leaveCall(Node n) throws SemanticException {
		cache.put(n.nodeId(), context().freeze());
		return super.leaveCall(n);
	    }
	};
	v = v.context(c);
	n.visit(v);
    }
}
