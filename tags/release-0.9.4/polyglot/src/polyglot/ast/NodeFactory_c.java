package polyglot.ext.jl.ast;

import polyglot.ast.*;
import polyglot.ast.Assert;
import polyglot.types.Flags;
import polyglot.types.Package;
import polyglot.types.Type;
import polyglot.types.Qualifier;
import polyglot.util.*;
import java.util.*;

/**
 * A <code>NodeFactory</code> constructs AST nodes.  All node construction
 * should go through this factory or by done with the <code>copy()</code>
 * method of <code>Node</code>.
 */
public class NodeFactory_c extends AbstractNodeFactory_c
{
    public AmbPrefix AmbPrefix(Position pos, Prefix prefix, String name) {
        return new AmbPrefix_c(null, null, pos, prefix, name);
    }

    public AmbReceiver AmbReceiver(Position pos, Prefix prefix, String name) {
        return new AmbReceiver_c(null, null, pos, prefix, name);
    }

    public AmbQualifierNode AmbQualifierNode(Position pos, QualifierNode qualifier, String name) {
        return new AmbQualifierNode_c(null, null, pos, qualifier, name);
    }

    public AmbExpr AmbExpr(Position pos, String name) {
        return new AmbExpr_c(null, null, pos, name);
    }

    public AmbTypeNode AmbTypeNode(Position pos, QualifierNode qualifier, String name) {
        return new AmbTypeNode_c(null, null, pos, qualifier, name);
    }

    public ArrayAccess ArrayAccess(Position pos, Expr base, Expr index) {
        return new ArrayAccess_c(null, null, pos, base, index);
    }

    public ArrayInit ArrayInit(Position pos, List elements) {
        return new ArrayInit_c(null, null, pos, elements);
    }

    public Assert Assert(Position pos, Expr cond, Expr errorMessage) {
        return new Assert_c(null, null, pos, cond, errorMessage);
    }

    public Assign Assign(Position pos, Expr left, Assign.Operator op, Expr right) {
        return new Assign_c(null, null, pos, left, op, right);
    }

    public Binary Binary(Position pos, Expr left, Binary.Operator op, Expr right) {
        return new Binary_c(null, null, pos, left, op, right);
    }

    public Block Block(Position pos, List statements) {
        return new Block_c(null, null, pos, statements);
    }

    public SwitchBlock SwitchBlock(Position pos, List statements) {
        return new SwitchBlock_c(null, null, pos, statements);
    }

    public BooleanLit BooleanLit(Position pos, boolean value) {
        return new BooleanLit_c(null, null, pos, value);
    }

    public Branch Branch(Position pos, Branch.Kind kind, String label) {
        return new Branch_c(null, null, pos, kind, label);
    }

    public Call Call(Position pos, Receiver target, String name, List args) {
        return new Call_c(null, null, pos, target, name, args);
    }

    public Case Case(Position pos, Expr expr) {
        return new Case_c(null, null, pos, expr);
    }

    public Cast Cast(Position pos, TypeNode type, Expr expr) {
        return new Cast_c(null, null, pos, type, expr);
    }

    public Catch Catch(Position pos, Formal formal, Block body) {
        return new Catch_c(null, null, pos, formal, body);
    }

    public CharLit CharLit(Position pos, char value) {
        return new CharLit_c(null, null, pos, value);
    }

    public ClassBody ClassBody(Position pos, List members) {
        return new ClassBody_c(null, null, pos, members);
    }

    public ClassDecl ClassDecl(Position pos, Flags flags, String name, TypeNode superClass, List interfaces, ClassBody body) {
        return new ClassDecl_c(null, null, pos, flags, name, superClass, interfaces, body);
    }

    public Conditional Conditional(Position pos, Expr cond, Expr consequent, Expr alternative) {
        return new Conditional_c(null, null, pos, cond, consequent, alternative);
    }

    public ConstructorCall ConstructorCall(Position pos, ConstructorCall.Kind kind, Expr outer, List args) {
        return new ConstructorCall_c(null, null, pos, kind, outer, args);
    }

    public ConstructorDecl ConstructorDecl(Position pos, Flags flags, String name, List formals, List throwTypes, Block body) {
        return new ConstructorDecl_c(null, null, pos, flags, name, formals, throwTypes, body);
    }

    public FieldDecl FieldDecl(Position pos, Flags flags, TypeNode type, String name, Expr init) {
        return new FieldDecl_c(null, null, pos, flags, type, name, init);
    }

    public Do Do(Position pos, Stmt body, Expr cond) {
        return new Do_c(null, null, pos, body, cond);
    }

    public Empty Empty(Position pos) {
        return new Empty_c(null, null, pos);
    }

    public Eval Eval(Position pos, Expr expr) {
        return new Eval_c(null, null, pos, expr);
    }

    public Field Field(Position pos, Receiver target, String name) {
        return new Field_c(null, null, pos, target, name);
    }

    public FloatLit FloatLit(Position pos, FloatLit.Kind kind, double value) {
        return new FloatLit_c(null, null, pos, kind, value);
    }

    public For For(Position pos, List inits, Expr cond, List iters, Stmt body) {
        return new For_c(null, null, pos, inits, cond, iters, body);
    }

    public Formal Formal(Position pos, Flags flags, TypeNode type, String name) {
        return new Formal_c(null, null, pos, flags, type, name);
    }

    public If If(Position pos, Expr cond, Stmt consequent, Stmt alternative) {
        return new If_c(null, null, pos, cond, consequent, alternative);
    }

    public Import Import(Position pos, Import.Kind kind, String name) {
        return new Import_c(null, null, pos, kind, name);
    }

    public Initializer Initializer(Position pos, Flags flags, Block body) {
        return new Initializer_c(null, null, pos, flags, body);
    }

    public Instanceof Instanceof(Position pos, Expr expr, TypeNode type) {
        return new Instanceof_c(null, null, pos, expr, type);
    }

    public IntLit IntLit(Position pos, IntLit.Kind kind, long value) {
        return new IntLit_c(null, null, pos, kind, value);
    }

    public Labeled Labeled(Position pos, String label, Stmt body) {
        return new Labeled_c(null, null, pos, label, body);
    }

    public Local Local(Position pos, String name) {
        return new Local_c(null, null, pos, name);
    }

    public LocalClassDecl LocalClassDecl(Position pos, ClassDecl decl) {
        return new LocalClassDecl_c(null, null, pos, decl);
    }

    public LocalDecl LocalDecl(Position pos, Flags flags, TypeNode type, String name, Expr init) {
        return new LocalDecl_c(null, null, pos, flags, type, name, init);
    }

    public MethodDecl MethodDecl(Position pos, Flags flags, TypeNode returnType, String name, List formals, List throwTypes, Block body) {
        return new MethodDecl_c(null, null, pos, flags, returnType, name, formals, throwTypes, body);
    }

    public New New(Position pos, Expr outer, TypeNode objectType, List args, ClassBody body) {
        return new New_c(null, null, pos, outer, objectType, args, body);
    }

    public NewArray NewArray(Position pos, TypeNode base, List dims, int addDims, ArrayInit init) {
        return new NewArray_c(null, null, pos, base, dims, addDims, init);
    }

    public NullLit NullLit(Position pos) {
        return new NullLit_c(null, null, pos);
    }

    public Return Return(Position pos, Expr expr) {
        return new Return_c(null, null, pos, expr);
    }

    public SourceCollection SourceCollection(Position pos, List sources) {
        return new SourceCollection_c(null, null, pos,  sources);
    }

    public SourceFile SourceFile(Position pos, PackageNode packageName, List imports, List decls) {
        return new SourceFile_c(null, null, pos, packageName, imports, decls);
    }

    public Special Special(Position pos, Special.Kind kind, TypeNode outer) {
        return new Special_c(null, null, pos, kind, outer);
    }

    public StringLit StringLit(Position pos, String value) {
        return new StringLit_c(null, null, pos, value);
    }

    public Switch Switch(Position pos, Expr expr, List elements) {
        return new Switch_c(null, null, pos, expr, elements);
    }

    public Synchronized Synchronized(Position pos, Expr expr, Block body) {
        return new Synchronized_c(null, null, pos, expr, body);
    }

    public Throw Throw(Position pos, Expr expr) {
        return new Throw_c(null, null, pos, expr);
    }

    public Try Try(Position pos, Block tryBlock, List catchBlocks, Block finallyBlock) {
        return new Try_c(null, null, pos, tryBlock, catchBlocks, finallyBlock);
    }

    public ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base) {
        return new ArrayTypeNode_c(null, null, pos, base);
    }

    public CanonicalTypeNode CanonicalTypeNode(Position pos, Type type) {
        if (! type.isCanonical()) {
	    throw new InternalCompilerError("Cannot construct a canonical " +
		"type node for a non-canonical type.");
	}

        return new CanonicalTypeNode_c(null, null, pos, type);
    }

    public PackageNode PackageNode(Position pos, Package p) {
        return new PackageNode_c(null, null, pos, p);
    }

    public Unary Unary(Position pos, Unary.Operator op, Expr expr) {
        return new Unary_c(null, null, pos, op, expr);
    }

    public While While(Position pos, Expr cond, Stmt body) {
        return new While_c(null, null, pos, cond, body);
    }
}