package polyglot.dispatch;

import polyglot.ast.Node;

public class Traverser {

    Object over;
    Object pre;
    Object post;
    
    private Traverser(Object over, Object pre, Object post) {
	this.over = over;
	this.pre = pre;
	this.post = post;
    }

    public static void preorder(Node n, Object v) {
	n.accept(new Traverser(null, v, null));
    }
    
    public static void postorder(Node n, Object v) {
	n.accept(new Traverser(null, null, v));
    }
    
    public static void prepost(Node n, Object pre, Object post) {
	n.accept(new Traverser(null, pre, post));
    }

    public Node visit(Node n) {
	if (over != null) {
	    Node m = n.accept(over);
	    if (m != null)
		return m;
	}

	if (pre != null)
	    n.accept(pre);

	Node m = n.acceptChildren(this);

	if (post != null)
	    return m.accept(post);

	return m;
    }
}
