package ibex.ast;

import ibex.types.IbexTypeSystem;
import ibex.types.RAnd_c;
import ibex.types.RSub_c;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.CFGBuilder;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/**
 * Tag interface for elements of a rule RHS.
 */
public class RhsMinus_c extends RhsBinary_c implements RhsMinus {
    public RhsMinus_c(Position pos, RhsExpr c1, RhsExpr c2) {
        super(pos, c1, c2);
    }
    
    public RhsMinus left(RhsExpr left) {
        return (RhsMinus) super.left(left);
    }

    public RhsMinus right(RhsExpr right) {
        return (RhsMinus) super.right(right);
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
        w.write(" -");
        w.allowBreak(type() == null || type().isPrimitive() ? 2 : 0, " ");
        printSubExpr(right, true, w, tr);
    }

    public String toString() {
        return left + " - " + right;
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        RhsExpr r = (RhsExpr) right.visit(new NodeVisitor() {
            @Override
            public Node override(Node n) {
                if (n instanceof RhsBind) {
                    return ((RhsBind) n).item();
                }
                if (n instanceof RhsAction) {
                    return ((RhsAction) n).item();
                }
                return null;
            }
        });
        RhsMinus_c n = (RhsMinus_c) this.right(r);
        return n.rhs(new RSub_c(ts, position(), left().rhs(), right.rhs())).type(left.type());
    }
}
    
