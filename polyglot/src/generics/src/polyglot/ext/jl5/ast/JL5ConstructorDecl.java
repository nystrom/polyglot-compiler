package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.ConstructorDecl;

public interface JL5ConstructorDecl extends ConstructorDecl{

    public boolean isCompilerGenerated();
    public JL5ConstructorDecl setCompilerGenerated(boolean val);

        
    public List paramTypes();
    public JL5ConstructorDecl paramTypes(List paramTypes);
    
    public List annotations();
    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();
}
