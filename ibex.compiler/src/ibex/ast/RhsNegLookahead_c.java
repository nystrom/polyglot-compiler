package ibex.ast;

import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.PrettyPrinter;

public class RhsNegLookahead_c extends RhsLookahead_c implements RhsNegLookahead {

    public RhsNegLookahead_c(Position pos, RhsExpr item) {
        super(pos, item);
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("!");
        printSubExpr(item, false, w, tr);
    }
    
    public String toString() {
        return "!" + item;
    }

}
