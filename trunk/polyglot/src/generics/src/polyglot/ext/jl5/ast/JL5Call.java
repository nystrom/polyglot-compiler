package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.Call;

public interface JL5Call extends Call {

    List typeArguments();
    JL5Call typeArguments(List args);
}
