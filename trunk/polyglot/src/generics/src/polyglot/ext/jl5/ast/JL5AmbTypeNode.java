package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.AmbTypeNode;

public interface JL5AmbTypeNode extends AmbTypeNode {

    List typeArguments();
    JL5AmbTypeNode typeArguments(List args);
}
