package polyglot.ext.jl5.visit;

import polyglot.ast.Node;
import polyglot.types.Context;
import polyglot.types.SemanticException;

public interface ApplicationCheck {

    Node applicationCheck(ApplicationChecker ac, Context ctx) throws SemanticException;
}
