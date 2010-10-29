package polyglot.dispatch;


import java.util.Map;

import polyglot.ast.MethodDecl_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Node_c;
import polyglot.ast.Stmt_c;
import polyglot.ast.Term;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.Ref;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.ErrorInfo;

public class ReachChecker extends Visitor {
    Job job;
    TypeSystem ts;
    NodeFactory nf;
    Map<Node, Ref<Boolean>> completes;

    public ReachChecker(Job job, TypeSystem ts, NodeFactory nf, 
     Map<Node, Ref<Boolean>> completes 
    ) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
	this.completes = completes;
    }

    public Node visit(Node_c n) {
	return (Node_c) acceptChildren(n);
    }
    
    Ref<Boolean> completesRef(Term n) {
	Ref<Boolean> r = completes.get(n);
	if (r == null) {
	    r = Types.<Boolean> lazyRef(null);
	    r.update(false);
	    completes.put(n, r);
	}
	return r;
    }

    public Node visit(MethodDecl_c n) {
	if (completesRef(n).get() && ! n.methodDef().returnType().get().isVoid() && n.body() != null) {
	    Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Missing return statement.", n.position());
	}
	return (Node_c) acceptChildren(n);
    }
    
    public Node visit(Stmt_c n) {
	if (!n.reachableRef().get())
	    Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Unreachable statement.", n.position());
	return (Node_c) acceptChildren(n);
    }
}
