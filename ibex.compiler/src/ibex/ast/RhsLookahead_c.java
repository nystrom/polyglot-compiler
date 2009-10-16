package ibex.ast;

import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
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
        TypeSystem ts = tc.typeSystem();
        return type(ts.Null());
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        if (neg)
            w.write("!");
        w.write("[");
        printSubExpr(item, false, w, tr);
        w.write("]");
    }

    public String toString() {
        return (neg ? "!" : "") + "[" + item + "]";
    }


}
