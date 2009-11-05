package ibex.ast;

import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

public class RhsStar_c extends RhsIteration_c implements RhsStar {

    public RhsStar_c(Position pos, RhsExpr item) {
        super(pos, item);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();
        return type(ts.arrayOf(item.type()));
    }
    
    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        printSubExpr(item, false, w, tr);
        w.write("*");
    }
    
    public String toString() {
        return item.toString() + "*";
    }

}
