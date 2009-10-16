package ibex.ast;

import polyglot.ast.Expr;

public interface RhsRange extends RhsExpr {

    Expr lo();
    RhsRange lo(Expr from);
    
    Expr hi();
    RhsRange hi(Expr to);


}
