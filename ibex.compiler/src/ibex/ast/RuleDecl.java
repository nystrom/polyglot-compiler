package ibex.ast;

import polyglot.ast.*;

public interface RuleDecl extends MethodDecl {
    RhsExpr rhs();
    RuleDecl rhs(RhsExpr e);
}
