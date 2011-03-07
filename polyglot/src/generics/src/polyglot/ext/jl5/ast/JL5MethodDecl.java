package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.MethodDecl;

public interface JL5MethodDecl extends MethodDecl {

    public boolean isCompilerGenerated();
    public JL5MethodDecl setCompilerGenerated(boolean val);
   
    public List<ParamTypeNode> paramTypes();
    public JL5MethodDecl paramTypes(List<ParamTypeNode> paramTypes);

    public List<AnnotationElem> annotations();
    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();
}
