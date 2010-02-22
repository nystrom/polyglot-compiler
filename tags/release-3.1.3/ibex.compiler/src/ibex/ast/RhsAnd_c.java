package ibex.ast;

import ibex.types.IbexTypeSystem;
import ibex.types.TupleType_c;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

/**
 * Tag interface for elements of a rule RHS.
 */
public class RhsAnd_c extends RhsBinary_c implements RhsAnd {
    public RhsAnd_c(Position pos, RhsExpr c1, RhsExpr c2) {
        super(pos, c1, c2);
    }
    
    public RhsAnd left(RhsExpr left) {
        return (RhsAnd) super.left(left);
    }

    public RhsAnd right(RhsExpr right) {
        return (RhsAnd) super.right(right);
    }

    public List<RhsExpr> allElements() {
        List l = new ArrayList();
        l.add(left());
        l.add(right());
        return l;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(left, true, w, tr);
        w.write(" &");
        w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, " ");
        printSubExpr(right, true, w, tr);
    }

    public String toString() {
        return left + " & " + right;
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        Type t1 = left().type();
        Type t2 = right().type();
        Type t;
        if ((t1.isVoid() || t1.isNull()) && (t2.isVoid() || t2.isNull()))
            t = ts.Void();
        else
            t = new TupleType_c(ts, position(), Types.ref(t1), Types.ref(t2));
        return type(t);
    }
}
    
