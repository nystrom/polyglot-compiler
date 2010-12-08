/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;

/**
 * A <code>NodeFactory</code> constructs AST nodes.  All node construction
 * should go through this factory or by done with the <code>copy()</code>
 * method of <code>Node</code>.
 */
public class NodeFactory_c extends AbstractNodeFactory_c
{
    public NodeFactory_c() { }

    public FlagsNode FlagsNode(Position pos, Flags flags) {
	    FlagsNode n = new FlagsNode_c(pos, flags);
	    
	    
	    return n;
    }
    public Id Id(Position pos, Name name) {
        Id n = new Id_c(pos, name);
        
        
        return n;
    }

    public QualifiedName QualifiedName(Position pos, Node prefix, Id name) {
	QualifiedName n = new QualifiedName_c(pos, prefix, name);
        
        
        return n;
    }
    
    public AmbReceiver AmbReceiver(Position pos, Node child) {
        AmbReceiver n = new AmbReceiver_c(pos, child);
        
        
        return n;
    }

    public AmbQualifierNode AmbQualifierNode(Position pos, Node child) {
        AmbQualifierNode n = new AmbQualifierNode_c(pos, child);
        
        
        return n;
    }
    
    public AmbExpr AmbExpr(Position pos, Node child) {
        AmbExpr n = new AmbExpr_c(pos, child);
        
        
        return n;
    }
    
    public AmbTypeNode AmbTypeNode(Position pos, Node child) {
        AmbTypeNode n = new AmbTypeNode_c(pos, child);
        
        
        return n;
    }

    public ArrayAccess ArrayAccess(Position pos, Expr base, Expr index) {
        ArrayAccess n = new ArrayAccess_c(pos, base, index);
        
        
        return n;
    }

    public ArrayInit ArrayInit(Position pos, List<Expr> elements) {
        ArrayInit n = new ArrayInit_c(pos, CollectionUtil.nonNullList(elements));
        
        
        return n;
    }

    public Assert Assert(Position pos, Expr cond, Expr errorMessage) {
        Assert n = new Assert_c(pos, cond, errorMessage);
        
        
        return n;
    }

    public Assign Assign(Position pos, Expr left, polyglot.ast.Assign.Operator op, Expr right) {
	return new Assign_c(pos, left, op, right);
    }


    public Binary Binary(Position pos, Expr left, polyglot.ast.Binary.Operator op, Expr right) {
        Binary n = new Binary_c(pos, left, op, right);
        
        
        return n;
    }

    public Block Block(Position pos, List<Stmt> statements) {
        Block n = new Block_c(pos, CollectionUtil.nonNullList(statements));
        
        
        return n;
    }

    public SwitchBlock SwitchBlock(Position pos, List<Stmt> statements) {
        SwitchBlock n = new SwitchBlock_c(pos, CollectionUtil.nonNullList(statements));
        
        
        return n;
    }

    public BooleanLit BooleanLit(Position pos, boolean value) {
        BooleanLit n = new BooleanLit_c(pos, value);
        
        
        return n;
    }

    public Branch Branch(Position pos, polyglot.ast.Branch.Kind kind, Id label) {
        Branch n = new Branch_c(pos, kind, label);
        
        
        return n;
    }

    public Call Call(Position pos, Receiver target, Id name, List<Expr> args) {
        Call n = new Call_c(pos, target, name, CollectionUtil.nonNullList(args));
        
        
        return n;
    }

    public Case Case(Position pos, Expr expr) {
        Case n = new Case_c(pos, expr);
        
        
        return n;
    }

    public Cast Cast(Position pos, TypeNode type, Expr expr) {
        Cast n = new Cast_c(pos, type, expr);
        
        
        return n;
    }

    public Catch Catch(Position pos, Formal formal, Block body) {
        Catch n = new Catch_c(pos, formal, body);
        
        
        return n;
    }

    public CharLit CharLit(Position pos, char value) {
        CharLit n = new CharLit_c(pos, value);
        
        
        return n;
    }

    public ClassBody ClassBody(Position pos, List<ClassMember> members) {
        ClassBody n = new ClassBody_c(pos, CollectionUtil.nonNullList(members));
        
        
        return n;
    }
    
    public ClassDecl ClassDecl(Position pos, FlagsNode flags, Id name, TypeNode superClass, List<TypeNode> interfaces, ClassBody body) {
        ClassDecl n = new ClassDecl_c(pos, flags, name, superClass, CollectionUtil.nonNullList(interfaces), body);
        
        
        return n;
    }

    public ClassLit ClassLit(Position pos, TypeNode typeNode) {
        ClassLit n = new ClassLit_c(pos, typeNode);
        
        
        return n;
    }

    public Conditional Conditional(Position pos, Expr cond, Expr consequent, Expr alternative) {
        Conditional n = new Conditional_c(pos, cond, consequent, alternative);
        
        
        return n;
    }

    public ConstructorCall ConstructorCall(Position pos, polyglot.ast.ConstructorCall.Kind kind, Expr outer, List<Expr> args) {
        ConstructorCall n = new ConstructorCall_c(pos, kind, outer, CollectionUtil.nonNullList(args));
        
        
        return n;
    }
    
    public ConstructorDecl ConstructorDecl(Position pos, FlagsNode flags, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        ConstructorDecl n = new ConstructorDecl_c(pos, flags, name, CollectionUtil.nonNullList(formals), CollectionUtil.nonNullList(throwTypes), body);
        
        
        return n;
    }

    public FieldDecl FieldDecl(Position pos, FlagsNode flags, TypeNode type, Id name, Expr init) {
        FieldDecl n = new FieldDecl_c(pos, flags, type, name, init);
        
        
        return n;
    }

    public Do Do(Position pos, Stmt body, Expr cond) {
        Do n = new Do_c(pos, body, cond);
        
        
        return n;
    }

    public Empty Empty(Position pos) {
        Empty n = new Empty_c(pos);
        
        
        return n;
    }

    public Eval Eval(Position pos, Expr expr) {
        Eval n = new Eval_c(pos, expr);
        
        
        return n;
    }
    
    public Field Field(Position pos, Receiver target, Id name) {
        Field n = new Field_c(pos, target, name);
        
        
        return n;
    }

    public FloatLit FloatLit(Position pos, polyglot.ast.FloatLit.Kind kind, double value) {
        FloatLit n = new FloatLit_c(pos, kind, value);
        
        
        return n;
    }

    public For For(Position pos, List<ForInit> inits, Expr cond, List<ForUpdate> iters, Stmt body) {
        For n = new For_c(pos, CollectionUtil.nonNullList(inits), cond, CollectionUtil.nonNullList(iters), body);
        
        
        return n;
    }

    public Formal Formal(Position pos, FlagsNode flags, TypeNode type, Id name) {
        Formal n = new Formal_c(pos, flags, type, name);
        
        
        return n;
    }
    
    public If If(Position pos, Expr cond, Stmt consequent, Stmt alternative) {
        If n = new If_c(pos, cond, consequent, alternative);
        
        
        return n;
    }
    
    public Import Import(Position pos, polyglot.ast.Import.Kind kind, QName name) {
        Import n = new Import_c(pos, kind, name);
        
        
        return n;
    }

    public Initializer Initializer(Position pos, FlagsNode flags, Block body) {
        Initializer n = new Initializer_c(pos, flags, body);
        
        
        return n;
    }

    public Instanceof Instanceof(Position pos, Expr expr, TypeNode type) {
        Instanceof n = new Instanceof_c(pos, expr, type);
        
        
        return n;
    }

    public IntLit IntLit(Position pos, polyglot.ast.IntLit.Kind kind, long value) {
        IntLit n = new IntLit_c(pos, kind, value);
        
        
        return n;
    }

    public Labeled Labeled(Position pos, Id label, Stmt body) {
        Labeled n = new Labeled_c(pos, label, body);
        
        
        return n;
    }

    public Local Local(Position pos, Id name) {
        Local n = new Local_c(pos, name);
        
        
        return n;
    }

    public LocalClassDecl LocalClassDecl(Position pos, ClassDecl decl) {
        LocalClassDecl n = new LocalClassDecl_c(pos, decl);
        
        
        return n;
    }
    
    public LocalDecl LocalDecl(Position pos, FlagsNode flags, TypeNode type, Id name, Expr init) {
        LocalDecl n = new LocalDecl_c(pos, flags, type, name, init);
        
        
        return n;
    }

    public MethodDecl MethodDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body) {
        MethodDecl n = new MethodDecl_c(pos, flags, returnType, name, CollectionUtil.nonNullList(formals), CollectionUtil.nonNullList(throwTypes), body);
        
        
        return n;
    }

    public New New(Position pos, Expr outer, TypeNode objectType, List<Expr> args, ClassBody body) {
        New n = new New_c(pos, outer, objectType, CollectionUtil.nonNullList(args), body);
        return n;
    }

    public NewArray NewArray(Position pos, TypeNode base, List dims, int addDims, ArrayInit init) {
        NewArray n = new NewArray_c(pos, base, CollectionUtil.nonNullList(dims), addDims, init);
        
        
        return n;
    }
    
    public NodeList NodeList(Position pos, NodeFactory nf, List nodes) {
        NodeList n = new NodeList_c(pos, nf, nodes);
        
        
        return n;
    }

    public NullLit NullLit(Position pos) {
        NullLit n = new NullLit_c(pos);
        
        
        return n;
    }

    public Return Return(Position pos, Expr expr) {
        Return n = new Return_c(pos, expr);
        
        
        return n;
    }

    public SourceCollection SourceCollection(Position pos, List sources) {
        SourceCollection n = new SourceCollection_c(pos, CollectionUtil.nonNullList(sources));
        
        
        return n;
    }

    public SourceFile SourceFile(Position pos, PackageNode packageName, List<Import> imports, List<TopLevelDecl> decls) {
        SourceFile n = new SourceFile_c(pos, packageName, CollectionUtil.nonNullList(imports), CollectionUtil.nonNullList(decls));
        
        
        return n;
    }

    public Special Special(Position pos, polyglot.ast.Special.Kind kind, TypeNode outer) {
         Special n = new Special_c(pos, kind, outer);
        
        
        return n;
    }

    public StringLit StringLit(Position pos, String value) {
        StringLit n = new StringLit_c(pos, value);
        
        
        return n;
    }

    public Switch Switch(Position pos, Expr expr, List elements) {
        Switch n = new Switch_c(pos, expr, CollectionUtil.nonNullList(elements));
        
        
        return n;
    }

    public Synchronized Synchronized(Position pos, Expr expr, Block body) {
        Synchronized n = new Synchronized_c(pos, expr, body);
        
        
        return n;
    }

    public Throw Throw(Position pos, Expr expr) {
        Throw n = new Throw_c(pos, expr);
        
        
        return n;
    }

    public Try Try(Position pos, Block tryBlock, List catchBlocks, Block finallyBlock) {
        Try n = new Try_c(pos, tryBlock, CollectionUtil.nonNullList(catchBlocks), finallyBlock);
        
        
        return n;
    }

    public ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base) {
        ArrayTypeNode n = new ArrayTypeNode_c(pos, base);
        
        
        return n;
    }

    public CanonicalTypeNode CanonicalTypeNode(Position pos, Ref<Type> type) {
        CanonicalTypeNode n = new CanonicalTypeNode_c(pos, type);
        
        
        return n;
    }

    public PackageNode PackageNode(Position pos, Ref<Package> p) {
        PackageNode n = new PackageNode_c(pos, p);
        
        
        return n;
    }

    public Unary Unary(Position pos, polyglot.ast.Unary.Operator op, Expr expr) {
        Unary n = new Unary_c(pos, op, expr);
        
        
        return n;
    }

    public While While(Position pos, Expr cond, Stmt body) {
        While n = new While_c(pos, cond, body);
        
        
        return n;
    }
}
