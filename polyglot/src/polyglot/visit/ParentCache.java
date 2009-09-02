package polyglot.visit;

import java.util.HashMap;
import java.util.Map;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;

public class ParentCache {
    protected Map<Node, Node> cache;
    protected Job job;
    protected TypeSystem ts;
    protected NodeFactory nf;

    public ParentCache(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
	this.cache = null;
    }

    public void invalidate() {
	cache = null;
    }
    
    public Node get(Node n) {
	if (cache != null) {
	    if (cache.containsKey(n)) {
		Node c = cache.get(n);
		return c;
	    }
	}

	recompute();

	if (cache.containsKey(n)) {
	    Node c = cache.get(n);
	    return c;
	}

	throw new InternalCompilerError("Could not find parent for " + n);
    }

    protected void recompute() {
	cache = new HashMap<Node, Node>();
	add(job.ast(), null);
    }

    public void add(Node n, Node parent) {
	if (n == null)
	    return;
	NodeVisitor v = new NodeVisitor() {
	    @Override
	    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
		cache.put(n, parent);
		return n;
	    }
	};
	if (parent != null)
	    parent.visitChild(n, v);
	else
	    n.visit(v);
    }
}
