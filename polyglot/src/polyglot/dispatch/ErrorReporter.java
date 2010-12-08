package polyglot.dispatch;

import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Node_c;
import polyglot.frontend.Globals;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.visit.NodeVisitor;

public class ErrorReporter {
    public ErrorReporter() {
    }

    public void accept(Node n) {
	if (n == null)
	    return;
	
	try {
	    n.accept(this);
	}
	catch (PassthruError e) {
	    throw new InternalCompilerError(e.getCause());
	}
    }

    public void accept(List<? extends Node> l) {
	for (Node n : l) {
	    accept(n);
	}
    }

    public Node acceptChildren(Node n) {
	return n.visitChildren(new NodeVisitor() {
	    public Node override(Node n) {
		ErrorReporter.this.accept(n);
		return n;
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
