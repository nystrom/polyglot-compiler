package ibex.ast;

import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.PrettyPrinter;

public class RhsPlusList_c extends RhsIterationList_c implements RhsPlusList {

    public RhsPlusList_c(Position pos, RhsExpr item, RhsExpr sep) {
        super(pos, item, sep);
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(item, true, w, tr);
        w.write(" ");
        w.write(" ++ ");
        w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, "");
        printSubExpr(sep, false, w, tr);
    }

    public String toString() {
        return item + " ++ " + sep;
    }
    

}
