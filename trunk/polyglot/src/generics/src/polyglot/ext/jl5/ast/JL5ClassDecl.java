package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.ClassDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.types.TypeSystem;

public interface JL5ClassDecl extends ClassDecl {

    public List annotations();

    public JL5ClassDecl annotations(List annotations);

    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();
    Node addDefaultConstructorIfNeeded(TypeSystem ts, NodeFactory nf);

    public List paramTypes();
}
