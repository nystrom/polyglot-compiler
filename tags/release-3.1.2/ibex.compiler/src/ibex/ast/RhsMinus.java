package ibex.ast;


/**
 * Merge node.
 */
public interface RhsMinus extends RhsExpr {
    RhsExpr left();
    RhsMinus left(RhsExpr left);

    RhsExpr right();
    RhsMinus right(RhsExpr right);
}
