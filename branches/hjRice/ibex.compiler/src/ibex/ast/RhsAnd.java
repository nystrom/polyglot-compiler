package ibex.ast;


/**
 * Merge node.
 */
public interface RhsAnd extends RhsExpr {
    RhsExpr left();
    RhsAnd left(RhsExpr left);

    RhsExpr right();
    RhsAnd right(RhsExpr right);
}
