package polyglot.dispatch;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.visit.NodeVisitor;

public abstract class Visitor {
    public Node accept(Node n, Object... args) {
	if (n == null)
	    return null;
	
	try {
	    Node m = n.accept(this, args);
	    return m;
	}
	catch (PassthruError e) {
	    if (e.getCause() instanceof SemanticException) {
		SemanticException x = (SemanticException) e.getCause();
		ErrorInfo error = new ErrorInfo(ErrorInfo.SEMANTIC_ERROR, x.getMessage() != null ? x.getMessage() : "unknown error",
		                                                                                 x.position() != null ? x.position() : n.position());
		return n.addError(error);
	    }
	    else {
		throw new InternalCompilerError(e.getCause());
	    }
	}
    }
    
    public List<? extends Node> accept(List<? extends Node> l, Object... args) {
	List<Node> result = new ArrayList<Node>();
	for (Node n : l) {
	    Node n2 = accept(n, args);
	    result.add(n2);
	}
	return result;
    }
    
    public Node acceptChildren(Node n, final Object... args) {
	return n.visitChildren(new NodeVisitor() {
	    public Node override(Node n) {
		return Visitor.this.accept(n, args);
	    }
	});
    }
    

}
