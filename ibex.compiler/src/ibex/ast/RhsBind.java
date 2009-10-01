package ibex.ast;

import polyglot.ast.LocalDecl;

public interface RhsBind extends RhsExpr {
    
    LocalDecl decl();
    RhsBind decl(LocalDecl decl);
}
