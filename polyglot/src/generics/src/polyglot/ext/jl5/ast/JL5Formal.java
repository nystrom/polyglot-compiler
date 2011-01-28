package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.Formal;

public interface JL5Formal extends Formal {

    List annotations();
    JL5Formal annotations(List annotations);

    boolean isVariable();
    
    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();
}
