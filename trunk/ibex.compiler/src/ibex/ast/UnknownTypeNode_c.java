package ibex.ast;

import polyglot.ast.TypeNode_c;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.PrettyPrinter;

public class UnknownTypeNode_c extends TypeNode_c {

    public UnknownTypeNode_c(Position pos) {
        super(pos);
    }

    @Override
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("_");
    }
}
