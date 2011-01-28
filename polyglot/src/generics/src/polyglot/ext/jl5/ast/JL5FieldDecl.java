package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.FieldDecl;

public interface JL5FieldDecl extends FieldDecl{

    public boolean isCompilerGenerated();
    public JL5FieldDecl setCompilerGenerated(boolean val);

    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();
}
