package ibex.visit;

import ibex.ast.IbexNodeFactory;
import ibex.lr.GLR;
import ibex.types.IbexClassDef;
import ibex.types.IbexTypeSystem;
import polyglot.ast.ClassDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;

/** Visitor which traverses the AST constructing type objects. */
public class ParserGenerator extends ContextVisitor
{
    public ParserGenerator(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    public IbexTypeSystem glrTypeSystem() {
        return (IbexTypeSystem) typeSystem();
    }

    public IbexNodeFactory glrNodeFactory() {
        return (IbexNodeFactory) nodeFactory();
    }

    protected Node leaveCall(Node old, Node n, NodeVisitor v) throws SemanticException {
        if (n instanceof ClassDecl) {
            ClassDecl cd = (ClassDecl) n;
            IbexClassDef pt = (IbexClassDef) cd.classDef();
            if (pt.isParser()) {
                GLR glr = new GLR((ibex.ExtensionInfo) job().extensionInfo(), pt);
                pt.setGLR(glr);
            }
        }

        return n;
    }
}
