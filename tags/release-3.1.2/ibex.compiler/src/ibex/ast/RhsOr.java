package ibex.ast;



public interface RhsOr extends RhsExpr {
    RhsExpr left();
    RhsOr left(RhsExpr left);

    RhsExpr right();
    RhsOr right(RhsExpr right);
}
