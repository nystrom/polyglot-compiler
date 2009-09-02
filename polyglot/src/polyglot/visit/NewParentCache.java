package polyglot.visit;

import java.util.HashMap;
import java.util.Map;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;

public class NewParentCache {
    protected Map<Integer, Integer> cache;
    protected Job job;

    public NewParentCache(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.cache = new HashMap<Integer, Integer>();
    }

    public void invalidate() {
	recompute();
    }
    
    public Integer get(Node n) {
	if (cache != null) {
	    if (cache.containsKey(n.nodeId())) {
		Integer c = cache.get(n.nodeId());
		if (c == -1)
		    return null;
		return c;
	    }
	}

	recompute();

	if (cache.containsKey(n.nodeId())) {
	    Integer c = cache.get(n.nodeId());
	    if (c == -1)
		return null;
	    return c;
	}

	throw new InternalCompilerError("Could not find parent for " + n);
    }

    protected void recompute() {
	cache = new HashMap<Integer, Integer>();
	add(job.ast(), null);
    }
    
    public void addChildren(Node n) {
	Integer p = get(n);
	if (p == null)
	    add(n, null);
	else
	    add(n, job.findById(p));
    }

    public void addSameParent(Node n, Node old) {
	Integer p = get(old);
	if (p == null)
	    add(n, null);
	else
	    add(n, job.findById(p));
    }

    public void add(Node n, Node parent) {
	if (n == null)
	    return;
	NodeVisitor v = new NodeVisitor() {
	    @Override
	    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
		if (parent != null)
		    cache.put(n.nodeId(), parent.nodeId());
		else
		    cache.put(n.nodeId(), -1);
		return n;
	    }
	};
	if (parent != null)
	    parent.visitChild(n, v);
	else
	    n.visit(v);
    }
}
