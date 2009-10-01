package ibex.ast;

import java.util.List;

public interface RhsSequence extends RhsExpr {

    List<RhsExpr> terms();
    RhsSequence terms(List<RhsExpr> terms);
}