package ibex.ast;

public interface RhsIterationList extends RhsExpr {
    public RhsExpr item();
    public RhsIterationList item(RhsExpr item);
    public RhsExpr sep();
    public RhsIterationList sep(RhsExpr sep);

}
