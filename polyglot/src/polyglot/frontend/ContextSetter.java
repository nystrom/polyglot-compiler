/**
 * 
 */
package polyglot.frontend;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.ContextVisitor;

public class ContextSetter extends ContextVisitor {
    public ContextSetter(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    @Override
    protected Node leaveCall(Node n) throws SemanticException {
        return n.context(context().freeze());
    }
}