package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.Formal;

public interface JL5Formal extends Formal {

    List<AnnotationElem> annotations();
    JL5Formal annotations(List<AnnotationElem> annotations);

    boolean isVarargs();
    
    public List runtimeAnnotations();
    public List classAnnotations();
    public List sourceAnnotations();
}
