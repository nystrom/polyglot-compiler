package ibex.ast;


import java.util.Arrays;
import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassMember;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.ast.LocalDecl;
import polyglot.ast.NodeFactory_c;
import polyglot.ast.TypeNode;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;

/**
 * NodeFactory for ibex extension.
 */
public class IbexNodeFactory_c extends NodeFactory_c implements IbexNodeFactory {
    @Override
    public polyglot.ast.ClassBody ClassBody(Position pos, List<ClassMember> members) {
        ClassBody n = new IbexClassBody_c(pos, CollectionUtil.nonNullList(members));
        n = (ClassBody)n.ext(extFactory().extClassBody());
        n = (ClassBody)n.del(delFactory().delClassBody());
        return n;
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
    public ibex.ast.RhsInvoke RhsInvoke(Position pos, Call e) {
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
    public ibex.ast.RhsLookahead RhsLookahead(Position pos, RhsExpr item) {
        return new RhsLookahead_c(pos, item, false);
    }
    public ibex.ast.RhsLookahead RhsNegLookahead(Position pos, RhsExpr item) {
        return new RhsLookahead_c(pos, item, true);
    }
    public ibex.ast.RhsLookahead RhsPosLookahead(Position pos, RhsExpr item) {
        return new RhsLookahead_c(pos, item, false);
    }
    public ibex.ast.RhsSequence RhsSequence(Position pos, List<RhsExpr> terms) {
        return new RhsSequence_c(pos, terms);
    }
    public ibex.ast.RhsSequence RhsSequence(Position pos, RhsExpr... terms) {
        return new RhsSequence_c(pos, Arrays.asList(terms));
    }
    public ibex.ast.RhsOr RhsOr(Position pos, RhsExpr c1, RhsExpr c2) {
        return new RhsOr_c(pos, c1, c2);
    }
    public ibex.ast.RhsExpr RhsOr(Position pos, List<RhsExpr> cases) {
        if (cases.size() == 0)
            return RhsSequence(pos, cases);
        if (cases.size() == 1)
            return cases.get(0);
        return RhsOr(pos, cases.get(0), RhsOr(pos, cases.subList(1, cases.size())));
    }
    public ibex.ast.RhsExpr RhsOrdered(Position pos, List<RhsExpr> cases) {
        if (cases.size() == 0)
            return RhsSequence(pos, cases);
        if (cases.size() == 1)
            return cases.get(0);
        return RhsOrdered(pos, cases.get(0), RhsOrdered(pos, cases.subList(1, cases.size())));
    }
    public ibex.ast.RhsAnd RhsAnd(Position pos, RhsExpr c1, RhsExpr c2) {
        return new RhsAnd_c(pos, c1, c2);
    }
    public ibex.ast.RhsMinus RhsMinus(Position pos, RhsExpr c1, RhsExpr c2) {
        return new RhsMinus_c(pos, c1, c2);
    }
    public ibex.ast.RhsOrdered RhsOrdered(Position pos, RhsExpr c1, RhsExpr c2) {
        return new RhsOrdered_c(pos, c1, c2);
    }
    public ibex.ast.RhsAction RhsAction(Position pos, RhsExpr item, Block body) {
        return new RhsAction_c(pos, item, body);
    }
    public RuleDecl RuleDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, List<RhsExpr> rhs) {
        return new RuleDecl_c(pos, flags, returnType, name, RhsOr(pos, rhs));
    }
    public RuleDecl RuleDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, RhsExpr rhs) {
        return new RuleDecl_c(pos, flags, returnType, name, rhs);
    }
}
