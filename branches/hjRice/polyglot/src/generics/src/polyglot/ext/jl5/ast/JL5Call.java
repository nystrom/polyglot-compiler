package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.Call;
import polyglot.ast.TypeNode;

public interface JL5Call extends Call {

    List<TypeNode> typeArguments();
    JL5Call typeArguments(List<TypeNode> args);
}
