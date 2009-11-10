package ibex.ast;

import ibex.ast.RhsAnyChar_c.RDummy_c;
import ibex.types.IbexTypeSystem;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
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
        IbexTypeSystem ts = (IbexTypeSystem) tc.typeSystem();
        Type t = ts.arrayOf(item.type());
        return rhs(new RDummy_c(ts, position(), t)).type(t);
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
