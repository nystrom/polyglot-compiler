/**
 * 
 */
package polyglot.frontend;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.SourceFile;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

public class ContextSetter extends ContextVisitor {
    public ContextSetter(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    @Override
    protected Node leaveCall(Node old, Node n, NodeVisitor v)
    throws SemanticException {

	if (v instanceof ContextVisitor) {
	    ContextVisitor cv = (ContextVisitor) v;
	    return n.context(cv.context().freeze());
	}
	
	return n.context(context().freeze());
    }
}