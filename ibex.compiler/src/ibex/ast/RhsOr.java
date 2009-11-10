package ibex.ast;

import java.util.List;



public interface RhsOr extends RhsExpr {
    List<RhsExpr> items();
    RhsOr items(List<RhsExpr> items);
}
