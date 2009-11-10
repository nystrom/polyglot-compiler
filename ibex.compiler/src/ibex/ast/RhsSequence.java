package ibex.ast;

import java.util.List;

public interface RhsSequence extends RhsExpr {

    List<RhsExpr> items();
    RhsSequence items(List<RhsExpr> terms);
}