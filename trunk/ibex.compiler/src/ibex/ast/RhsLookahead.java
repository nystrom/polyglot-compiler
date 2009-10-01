package ibex.ast;

public interface RhsLookahead extends RhsExpr {
    RhsExpr item();
    RhsLookahead item(RhsExpr item);
}
