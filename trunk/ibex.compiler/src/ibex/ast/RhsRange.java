package ibex.ast;

import polyglot.ast.Expr;

public interface RhsRange extends RhsExpr {

    Expr from();
    RhsRange from(Expr from);
    
    Expr to();
    RhsRange to(Expr to);


}
