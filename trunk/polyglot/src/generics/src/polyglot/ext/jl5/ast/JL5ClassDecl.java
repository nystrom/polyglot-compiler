package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.ClassDecl;

public interface JL5ClassDecl extends ClassDecl {

    public List<AnnotationElem> annotations();

    public JL5ClassDecl annotations(List<AnnotationElem> annotations);

    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();

    public List<ParamTypeNode> paramTypes();
}
