package ibex.ast;

import polyglot.ast.Expr;

public interface RhsExpr extends Expr {
    boolean isRegular();
    RhsExpr isRegular(boolean f);
    

    public Object rhs();
    public RhsExpr rhs(Object rhs);
}
