package ibex.ast;


import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.ast.LocalDecl;
import polyglot.ast.NodeFactory_c;
import polyglot.ast.TypeNode;
import polyglot.util.Position;

/**
 * NodeFactory for ibex extension.
 */
public class IbexNodeFactory_c extends NodeFactory_c implements IbexNodeFactory {
    public MatchContext MatchContext(Position pos) {
        return new MatchContext_c(pos);
    }
    public RhsAnyChar RhsAnyChar(Position pos) {
        return new RhsAnyChar_c(pos);
    }
    public RhsBind RhsBind(Position pos, LocalDecl decl) {
        return new RhsBind_c(pos, decl);
    }
    public ibex.ast.RhsLit RhsLit(Position pos, Expr lit) {
        return new RhsLit_c(pos, lit);
    }
    public ibex.ast.RhsInvoke RhsSymbol(Position pos, Call e) {
        return new RhsInvoke_c(pos, e);
    }
    public ibex.ast.RhsRange RhsRange(Position pos, Expr from, Expr to) {
        return new RhsRange_c(pos, from, to);
    }
    public ibex.ast.RhsPlusList RhsPlusList(Position pos, RhsExpr item, RhsExpr sep) {
        return new RhsPlusList_c(pos, item, sep);
    }
    public ibex.ast.RhsStarList RhsStarList(Position pos, RhsExpr item, RhsExpr sep) {
        return new RhsStarList_c(pos, item, sep);
    }
    public ibex.ast.RhsStar RhsStar(Position pos, RhsExpr item) {
        return new RhsStar_c(pos, item);
    }
    public ibex.ast.RhsPlus RhsPlus(Position pos, RhsExpr item) {
        return new RhsPlus_c(pos, item);
    }
    public ibex.ast.RhsOption RhsOption(Position pos, RhsExpr item) {
        return new RhsOption_c(pos, item);
    }
    public ibex.ast.RhsNegLookahead RhsNegLookahead(Position pos, RhsExpr item) {
        return new RhsNegLookahead_c(pos, item);
    }
    public ibex.ast.RhsPosLookahead RhsPosLookahead(Position pos, RhsExpr item) {
        return new RhsPosLookahead_c(pos, item);
    }
    public ibex.ast.RhsSequence RhsSequence(Position pos, List<RhsExpr> terms) {
        return new RhsSequence_c(pos, terms);
    }
    public ibex.ast.RhsCase RhsCase(Position pos, List<RhsExpr> cases) {
        return new RhsCase_c(pos, cases);
    }
    public ibex.ast.RhsAction RhsAction(Position pos, RhsExpr item, Block body) {
        return new RhsAction_c(pos, item, body);
    }
    public RuleDecl RuleDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, RhsExpr rhs) {
        return new RuleDecl_c(pos, flags, returnType, name, rhs);
    }
}
