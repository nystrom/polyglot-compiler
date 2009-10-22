package ibex.ast;


public interface RhsIteration extends RhsExpr {
    public RhsExpr item();
    public RhsIteration item(RhsExpr item);
}
