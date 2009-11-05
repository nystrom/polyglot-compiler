package ibex.ast;

import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsOrdered_c extends RhsBinary_c implements RhsOrdered {

    public RhsOrdered_c(Position pos, RhsExpr c1, RhsExpr c2) {
        super(pos, c1, c2);
    }

    public RhsOrdered left(RhsExpr left) {
        return (RhsOrdered) super.left(left);
    }

    public RhsOrdered right(RhsExpr right) {
        return (RhsOrdered) super.right(right);
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        return RhsOr_c.typeCheckOr(this, left, right, tc);
    }
    
    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
            printSubExpr(left, true, w, tr);
            w.write(" /");
            w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, " ");
            printSubExpr(right, true, w, tr);
    }

    public String toString() {
        return left + " / " + right;
    }
}
