package ibex.ast;

import ibex.types.RuleDef;
import polyglot.ast.*;

public interface RuleDecl extends MethodDecl {
    RuleDef rule();
    RuleDecl rule(RuleDef rule);

    TypeNode type();
    RuleDecl type(TypeNode type);

    RhsExpr rhs();
    RuleDecl rhs(RhsExpr e);
}
