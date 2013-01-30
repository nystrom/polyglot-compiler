package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.ConstructorDecl;

public interface JL5ConstructorDecl extends ConstructorDecl{

    public boolean isCompilerGenerated();
    public JL5ConstructorDecl setCompilerGenerated(boolean val);

        
    public List<ParamTypeNode> paramTypes();
    public JL5ConstructorDecl paramTypes(List<ParamTypeNode> paramTypes);
    
    public List<AnnotationElem> annotations();
    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();
}
