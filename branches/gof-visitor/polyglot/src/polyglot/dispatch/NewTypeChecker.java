package polyglot.dispatch;

import java.util.HashMap;
import java.util.Map;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.visit.ContextCache;
import polyglot.visit.TypeChecker;

public class NewTypeChecker extends NewVisitor {
    Job job;
    TypeSystem ts;
    NodeFactory nf;
    Map<Node, Node> memo;

    Context makeContext(Node parent, Node n) {
	return new ContextCache(job, ts, nf).get(n);
    }

    public Node visit(Node parent, Node n) {
	Context c = makeContext(parent, n);
	TypeChecker tc = new TypeChecker(job, ts, nf, new HashMap<Node, Node>());
	tc = (TypeChecker) tc.context(c);
	try {
	    Node m = n.del().typeCheckOverride(parent, tc);
	    if (m != null)
		return m;
	    n = n.del().visitChildren(tc);
	    n = n.del().disambiguate(tc);
	    n = n.del().typeCheck(tc);
	    n = n.del().checkConstants(tc);
	}
	catch (SemanticException e) {
	    return n;
	}
	return n;
    }
}
