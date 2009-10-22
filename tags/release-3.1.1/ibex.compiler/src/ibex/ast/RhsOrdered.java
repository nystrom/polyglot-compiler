package ibex.ast;



public interface RhsOrdered extends RhsExpr {
    RhsExpr left();
    RhsOrdered left(RhsExpr left);

    RhsExpr right();
    RhsOrdered right(RhsExpr right);
}
