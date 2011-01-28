package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.MethodDecl;

public interface JL5MethodDecl extends MethodDecl {

    public boolean isCompilerGenerated();
    public JL5MethodDecl setCompilerGenerated(boolean val);
   
    public List paramTypes();
    public JL5MethodDecl paramTypes(List paramTypes);

    public List annotations();
    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();
}
