package polyglot.ext.jl5.visit;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/** Visitor which performs type checking on the AST. */
public class ApplicationChecker extends ContextVisitor
{
    public ApplicationChecker(Job job, TypeSystem ts, NodeFactory nf) {
	    super(job, ts, nf);
    }

    protected Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        if (n instanceof ApplicationCheck){
	        return ((ApplicationCheck)n).applicationCheck((ApplicationChecker) v);
        }
	    return n;
    }
}
