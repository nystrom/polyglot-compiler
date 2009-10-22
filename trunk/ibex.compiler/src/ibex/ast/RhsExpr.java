package ibex.ast;

import ibex.types.Rhs;
import polyglot.ast.Expr;

public interface RhsExpr extends Expr {
    boolean isRegular();
    RhsExpr isRegular(boolean f);
    

    public Rhs rhs();
    public RhsExpr rhs(Rhs rhs);
}
