package ibex.ast;

import ibex.types.ActionDef;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Return;
import polyglot.ast.Return_c;
import polyglot.types.CodeDef;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.UnknownType;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class IbexReturn_c extends Return_c implements Return {

    public IbexReturn_c(Position pos, Expr expr) {
        super(pos, expr);
    }

    @Override
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        CodeDef cd = tc.context().currentCode();
        TypeSystem ts = tc.typeSystem();
        Type etype = expr != null ? expr.type() : ts.Void();
        if (cd instanceof ActionDef) {
            ActionDef ad = (ActionDef) cd;
            Type t = ad.returnType().get();
            if (t instanceof UnknownType) {
                ((Ref<Type>) ad.returnType()).update(etype);
            }
            else {
                if (t.isVoid() && expr != null)
                    throw new SemanticException("Cannot return value from void action.", position());
                if (! t.isVoid() && expr == null)
                    throw new SemanticException("Missing return value for action.", position());
                Type u = ts.leastCommonAncestor(t, etype, tc.context());
                ((Ref<Type>) ad.returnType()).update(u);
            }
            return this;
        }
        return super.typeCheck(tc);
    }
}
