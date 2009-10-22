package ibex.ast;

public interface RhsLookahead extends RhsExpr {
    RhsExpr item();

    RhsLookahead item(RhsExpr item);

    public boolean negativeLookahead();

    public RhsLookahead negativeLookahead(boolean f);
}
