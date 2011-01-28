package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.LocalDecl;

public interface JL5LocalDecl extends LocalDecl {

    List annotations();
    JL5LocalDecl annotations(List annotations);
}
