package polyglot.dispatch;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.*;
import polyglot.frontend.Globals;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.visit.NodeVisitor;

public class ErrorReporter {
    public ErrorReporter() {
    }

    public Node accept(Node n) {
	if (n == null)
	    return null;
	
	try {
	    Node m = n.accept(this);
	    return m;
	}
	catch (PassthruError e) {
	    throw new InternalCompilerError(e.getCause());
	}
    }

    public List<? extends Node> accept(List<? extends Node> l) {
	List result = new ArrayList();
	for (Node n : l) {
	    Node n2 = accept(n);
	    result.add(n2);
	}
	return result;
    }

    public Node acceptChildren(Node n) {
	return n.visitChildren(new NodeVisitor() {
	    public Node override(Node n) {
		return ErrorReporter.this.accept(n);
	    }
	});
    }

    public Node visit(Node_c n) {
//	System.out.println("missing node " + n + " instanceof " + n.getClass().getName());
	for (ErrorInfo e : n.errors()) {
	    Globals.Compiler().errorQueue().enqueue(e);
	}
	acceptChildren(n);
	return n;
    }
}
