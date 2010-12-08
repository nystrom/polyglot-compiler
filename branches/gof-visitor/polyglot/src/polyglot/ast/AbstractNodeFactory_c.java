/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.*;

import polyglot.types.*;
import polyglot.util.Position;

/**
 * This is a node factory that creates no nodes.  It, rather than
 * NodeFactory_c, should be subclassed by any extension which should
 * override the creation of <a>all</a> nodes.
 */
public abstract class AbstractNodeFactory_c implements NodeFactory
{
    public Id Id(Position pos, String name) {
	return Id(pos, Name.make(name));
    }
    
    public QualifiedName PrefixFromQualifiedName(Position pos, QName qualifiedName) {
	if (qualifiedName.qualifier() == null)
            return QualifiedName(pos, null, Id(pos, qualifiedName.name()));
        
        Position pos2 = pos.truncateEnd(qualifiedName.name().toString().length()+1);
        
        return QualifiedName(pos, PrefixFromQualifiedName(pos2, qualifiedName.qualifier()), Id(pos, qualifiedName.name()));
    }
    
    public TypeNode TypeNodeFromQualifiedName(Position pos, QName qualifiedName) {
	if (qualifiedName.qualifier() == null)
            return AmbTypeNode(pos, Id(pos, qualifiedName.name()));
        return AmbTypeNode(pos, PrefixFromQualifiedName(pos, qualifiedName));
    }
    
    public Receiver ReceiverFromQualifiedName(Position pos, QName qualifiedName) {
	if (qualifiedName.qualifier() == null)
	    return AmbReceiver(pos, Id(pos, qualifiedName.name()));
        return AmbReceiver(pos, PrefixFromQualifiedName(pos, qualifiedName));
  
    }
    
    public Expr ExprFromQualifiedName(Position pos, QName qualifiedName) {
	if (qualifiedName.qualifier() == null)
	    return AmbExpr(pos, Id(pos, qualifiedName.name()));
        return AmbExpr(pos, PrefixFromQualifiedName(pos, qualifiedName));
    }
    
    public QualifierNode QualifierNodeFromQualifiedName(Position pos, QName qualifiedName) {
	if (qualifiedName.qualifier() == null)
	    return AmbQualifierNode(pos, Id(pos, qualifiedName.name()));
        return AmbQualifierNode(pos, PrefixFromQualifiedName(pos, qualifiedName));
    }
    
    public CanonicalTypeNode CanonicalTypeNode(Position pos, Type type) {
        return CanonicalTypeNode(pos, Types.<Type>ref(type));
    }

    public final QualifiedName QualifiedName(Position pos, Id name) {
        return QualifiedName(pos, null, name);
    }

    public final ArrayInit ArrayInit(Position pos) {
	return ArrayInit(pos, Collections.<Expr>emptyList());
    }

    public final Assert Assert(Position pos, Expr cond) {
        return Assert(pos, cond, null);
    }

    public final Block Block(Position pos, Stmt... s) {
	return Block(pos, Arrays.asList(s));
    }

    public final Branch Break(Position pos) {
	return Branch(pos, Branch.BREAK, (Id) null);
    }

    public final Branch Break(Position pos, Id label) {
        return Branch(pos, Branch.BREAK, label);
    }

    public final Branch Continue(Position pos) {
	return Branch(pos, Branch.CONTINUE, (Id) null);
    }

    public final Branch Continue(Position pos, Id label) {
        return Branch(pos, Branch.CONTINUE, label);
    }

    public final Branch Branch(Position pos, polyglot.ast.Branch.Kind kind) {
	return Branch(pos, kind, (Id) null);
    }
    
    public final Call Call(Position pos, Id name, Expr... a1) {
        return Call(pos, null, name, Arrays.asList(a1));
    }

    public final Call Call(Position pos, Id name, List<Expr> args) {
        return Call(pos, null, name, args);
    }
    
    public final Call Call(Position pos, Receiver target, Id name, Expr... a1) {
        return Call(pos, target, name, Arrays.asList(a1));
    }

    public final Case Default(Position pos) {
	return Case(pos, null);
    }

    public final ConstructorCall ThisCall(Position pos, List<Expr> args) {
	return ConstructorCall(pos, ConstructorCall.THIS, null, args);
    }

    public final ConstructorCall ThisCall(Position pos, Expr outer, List<Expr> args) {
	return ConstructorCall(pos, ConstructorCall.THIS, outer, args);
    }

    public final ConstructorCall SuperCall(Position pos, List<Expr> args) {
	return ConstructorCall(pos, ConstructorCall.SUPER, null, args);
    }

    public final ConstructorCall SuperCall(Position pos, Expr outer, List<Expr> args) {
	return ConstructorCall(pos, ConstructorCall.SUPER, outer, args);
    }

    public final ConstructorCall ConstructorCall(Position pos, polyglot.ast.ConstructorCall.Kind kind, List<Expr> args) {
	return ConstructorCall(pos, kind, null, args);
    }

    public final FieldDecl FieldDecl(Position pos, FlagsNode flags, TypeNode type, Id name) {
        return FieldDecl(pos, flags, type, name, null);
    }

    public final Field Field(Position pos, Id name) {
        return Field(pos, null, name);
    }

    public final If If(Position pos, Expr cond, Stmt consequent) {
	return If(pos, cond, consequent, null);
    }

    public final LocalDecl LocalDecl(Position pos, FlagsNode flags, TypeNode type, Id name) {
        return LocalDecl(pos, flags, type, name, null);
    }

    public final New New(Position pos, TypeNode type, List<Expr> args) {
        return New(pos, null, type, args, null);
    }

    public final New New(Position pos, TypeNode type, List<Expr> args, ClassBody body) {
	return New(pos, null, type, args, body);
    }

    public final New New(Position pos, Expr outer, TypeNode objectType, List<Expr> args) {
        return New(pos, outer, objectType, args, null);
    }

    public final NewArray NewArray(Position pos, TypeNode base, List<Expr> dims) {
	return NewArray(pos, base, dims, 0, null);
    }

    public final NewArray NewArray(Position pos, TypeNode base, List<Expr> dims, int addDims) {
	return NewArray(pos, base, dims, addDims, null);
    }

    public final NewArray NewArray(Position pos, TypeNode base, int addDims, ArrayInit init) {
	return NewArray(pos, base, Collections.<Expr>emptyList(), addDims, init);
    }
    
    public final NodeList NodeList(Position pos, List<Node> nodes) {
        return NodeList(pos, this, nodes);
    }

    public final Return Return(Position pos) {
	return Return(pos, null);
    }

    public final SourceFile SourceFile(Position pos, List<TopLevelDecl> decls) {
        return SourceFile(pos, null, Collections.<Import>emptyList(), decls);
    }

    public final SourceFile SourceFile(Position pos, List<Import> imports, List<TopLevelDecl> decls) {
        return SourceFile(pos, null, imports, decls);
    }

    public final Special This(Position pos) {
        return Special(pos, Special.THIS, null);
    }

    public final Special This(Position pos, TypeNode outer) {
        return Special(pos, Special.THIS, outer);
    }

    public final Special Super(Position pos) {
        return Special(pos, Special.SUPER, null);
    }

    public final Special Super(Position pos, TypeNode outer) {
        return Special(pos, Special.SUPER, outer);
    }

    public final Special Special(Position pos, polyglot.ast.Special.Kind kind) {
        return Special(pos, kind, null);
    }

    public final Try Try(Position pos, Block tryBlock, List<Catch> catchBlocks) {
        return Try(pos, tryBlock, catchBlocks, null);
    }

    public final Unary Unary(Position pos, Expr expr, polyglot.ast.Unary.Operator op) {
        return Unary(pos, op, expr);
    }
}
