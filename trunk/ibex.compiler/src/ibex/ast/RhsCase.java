package ibex.ast;

import java.util.List;


public interface RhsCase extends RhsExpr {

    List<RhsExpr> cases();
    RhsCase cases(List<RhsExpr> cases);

}
