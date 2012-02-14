package polyglot.ext.jl5.visit;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ext.jl5.ast.JL5Import;
import polyglot.ext.jl5.types.JL5ImportTable;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.InitImportsVisitor;
import polyglot.visit.NodeVisitor;

public class JL5InitImportsVisitor extends InitImportsVisitor {

    public JL5InitImportsVisitor(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    public Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        if (n instanceof JL5Import) {
            JL5Import im = (JL5Import) n;
            
            if (im.kind() == JL5Import.ALL_STATIC_MEMBERS) {
                ((JL5ImportTable)this.importTable).addOnDemandStaticImport(im.name(), im.position());
            }
            else if (im.kind() == JL5Import.STATIC_MEMBER) {
                ((JL5ImportTable)this.importTable).addExplicitStaticImport(im.name(), im.position());
            }
        }
        return super.leaveCall(old, n, v);
    }
}
