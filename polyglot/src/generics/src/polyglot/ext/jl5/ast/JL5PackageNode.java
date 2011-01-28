package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.PackageNode;

public interface JL5PackageNode extends PackageNode {

    List annotations();
    JL5PackageNode annotations(List annotations);
}

