package ibex.ast;

import ibex.ast.RhsAnyChar_c.RDummy_c;
import ibex.types.IbexTypeSystem;
import ibex.types.RLookahead_c;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsLookahead_c extends RhsUnary_c implements RhsLookahead {
    private boolean neg;

    public RhsLookahead_c(Position pos, RhsExpr item, boolean neg) {
        super(pos, item);
        this.neg = neg;
    }
    
    public boolean negativeLookahead() {
        return neg;
    }
    
    public RhsLookahead negativeLookahead(boolean f) {
        RhsLookahead_c n = (RhsLookahead_c) copy();
        n.neg = f;
        return n;
    }
    
    public RhsLookahead item(RhsExpr item) {
        return (RhsLookahead) super.item(item);
    }
    
    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        // Remove any actions or bindings.
        RhsLookahead_c n = (RhsLookahead_c) this.visit(new NodeVisitor() {
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
        return n.rhs(new RLookahead_c(ts, position(), item.rhs(), neg)).type(ts.Void());
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        if (neg)
            w.write("~");
        w.write("[");
        printSubExpr(item, false, w, tr);
        w.write("]");
    }

    public String toString() {
        return (neg ? "~" : "") + "[" + item + "]";
    }


}
