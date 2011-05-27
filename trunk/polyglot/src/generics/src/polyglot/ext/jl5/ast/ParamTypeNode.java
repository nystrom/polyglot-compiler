package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.TypeNode;

public interface ParamTypeNode extends TypeNode {

    ParamTypeNode id(String id);
    String id();

    List<TypeNode> bounds();
    ParamTypeNode bounds(List<TypeNode> l);
}
