package ibex.ast;

import java.util.List;

import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.Id;
import polyglot.ast.LocalDecl;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.util.Position;

/**
 * NodeFactory for ibex extension.
 */
public interface IbexNodeFactory extends NodeFactory {
    RhsBind RhsBind(Position pos, LocalDecl decl); 
    RhsBind RhsSyntheticBind(Position pos, LocalDecl decl); 
    RhsAnyChar RhsAnyChar(Position pos);
    RhsLit RhsLit(Position pos, Expr lit);
    RhsRange RhsRange(Position pos, Expr from, Expr to);
    RhsInvoke RhsInvoke(Position pos, Call e);
    RhsPlusList RhsPlusList(Position pos, RhsExpr item, RhsExpr sep);
    RhsStarList RhsStarList(Position pos, RhsExpr item, RhsExpr sep);
    RhsStar RhsStar(Position pos, RhsExpr item);
    RhsPlus RhsPlus(Position pos, RhsExpr item);
    RhsOption RhsOption(Position pos, RhsExpr item);
    RhsLookahead RhsPosLookahead(Position pos, RhsExpr item);
    RhsLookahead RhsNegLookahead(Position pos, RhsExpr item);
    RhsLookahead RhsLookahead(Position pos, RhsExpr item);
    RhsSequence RhsSequence(Position pos, List<RhsExpr> terms);
    RhsSequence RhsSequence(Position pos, RhsExpr... terms);
    
    RhsOr RhsOr(Position pos, RhsExpr c1, RhsExpr c2);
    RhsOrdered RhsOrdered(Position pos, RhsExpr c1, RhsExpr c2);
    
    RhsExpr RhsOr(Position pos, List<RhsExpr> cases);
    RhsExpr RhsOrdered(Position pos, List<RhsExpr> cases);
    
    RhsAnd RhsAnd(Position pos, RhsExpr c1, RhsExpr c2);
    RhsMinus RhsMinus(Position pos, RhsExpr c1, RhsExpr c2);
    
    RhsAction RhsAction(Position pos, RhsExpr item, Block body);

    RuleDecl RuleDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, RhsExpr rhs);
    RuleDecl RuleDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, List<RhsExpr> rhs);
}
