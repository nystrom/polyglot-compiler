package ibex.ast;

import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.ast.LocalDecl;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.util.Position;

/**
 * NodeFactory for ibex extension.
 */
public interface IbexNodeFactory extends NodeFactory {
    MatchContext MatchContext(Position pos);
    
    RhsBind RhsBind(Position pos, LocalDecl decl); 
    RhsAnyChar RhsAnyChar(Position pos);
    RhsLit RhsLit(Position pos, Expr lit);
    RhsRange RhsRange(Position pos, Expr from, Expr to);
    RhsInvoke RhsSymbol(Position pos, Call e);
    RhsPlusList RhsPlusList(Position pos, RhsExpr item, RhsExpr sep);
    RhsStarList RhsStarList(Position pos, RhsExpr item, RhsExpr sep);
    RhsStar RhsStar(Position pos, RhsExpr item);
    RhsPlus RhsPlus(Position pos, RhsExpr item);
    RhsOption RhsOption(Position pos, RhsExpr item);
    RhsNegLookahead RhsNegLookahead(Position pos, RhsExpr item);
    RhsPosLookahead RhsPosLookahead(Position pos, RhsExpr item);
    RhsSequence RhsSequence(Position pos, List<RhsExpr> terms);
    RhsCase RhsCase(Position pos, List<RhsExpr> cases);
    RhsAction RhsAction(Position pos, RhsExpr item, Block body);

    RuleDecl RuleDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, RhsExpr rhs);
}
