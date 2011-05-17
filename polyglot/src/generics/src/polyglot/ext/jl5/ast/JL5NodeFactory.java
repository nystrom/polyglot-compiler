package polyglot.ext.jl5.ast;

import java.util.List;

import polyglot.ast.AmbQualifierNode;
import polyglot.ast.AmbTypeNode;
import polyglot.ast.ArrayInit;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ConstructorCall;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.Import;
import polyglot.ast.NodeFactory;
import polyglot.ast.PackageNode;
import polyglot.ast.Prefix;
import polyglot.ast.Receiver;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.types.ClassDef.Kind;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.util.Position;

/**
 * NodeFactory for jl5 extension.
 */
public interface JL5NodeFactory extends NodeFactory {
    // TODO: Declare any factory methods for new AST nodes.
    public ExtendedFor ExtendedFor(Position pos, List varDecls, Expr expr, Stmt stmt);
    public EnumConstantDecl EnumConstantDecl(Position pos, FlagAnnotations flags, Id name, List args, ClassBody body);
    public EnumConstantDecl EnumConstantDecl(Position pos, FlagAnnotations flags, Id name, List args);
    public JL5ClassDecl JL5ClassDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType, List interfaces, ClassBody body, List paramTypes);
    public JL5ClassBody JL5ClassBody(Position pos, List members);
    public JL5ConstructorDecl JL5ConstructorDecl(Position pos, FlagAnnotations flags, Id name, List formals, List throwTypes, Block body);
    public JL5ConstructorDecl JL5ConstructorDecl(Position pos, FlagAnnotations flags, Id name, List formals, List throwTypes, Block body, List typeParams);

    public Field Field(Position pos, Receiver target, Id name);

    public JL5Case JL5Case(Position pos, Expr expr);

    public JL5MethodDecl JL5MethodDecl(Position pos, FlagAnnotations flags, TypeNode returnType, Id name, List formals, List throwTypes, Block body, List typeParams);


    public AnnotationElemDecl AnnotationElemDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr def);
    
    public NormalAnnotationElem NormalAnnotationElem(Position pos, TypeNode name, List elements);
    public MarkerAnnotationElem MarkerAnnotationElem(Position pos, TypeNode name);
    public SingleElementAnnotationElem SingleElementAnnotationElem(Position pos, TypeNode name, Expr value);

    public ElementValuePair ElementValuePair(Position pos, Id name, Expr value);

    public polyglot.ast.ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base, boolean varargs);
    
    public JL5FieldDecl JL5FieldDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr init);
    
    public JL5Formal JL5Formal(Position pos, FlagAnnotations flags, TypeNode type, Id name);
    
    public JL5LocalDecl JL5LocalDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr init);

    public PackageNode PackageNode(Position pos, Ref<? extends Package> p);

    public JL5PackageNode JL5PackageNode(Position pos, FlagAnnotations fl, Ref<? extends Package> p);

    public ParamTypeNode ParamTypeNode(Position pos, List bounds, String id);
    
    public BoundedTypeNode BoundedTypeNode(Position pos, Kind kind, TypeNode bound);

    public AmbQualifierNode JL5AmbQualifierNode(Position pos, Prefix qual, Id name, List args);
    
    public AmbTypeNode JL5AmbTypeNode(Position pos, Prefix qual, Id name, List args);

    public ConstructorCall JL5ThisCall(Position pos, List args, List typeArgs);

    public ConstructorCall JL5ThisCall(Position pos, Expr outer, List args, List typeArgs);

    public ConstructorCall JL5SuperCall(Position pos, List args, List typeArgs);

    public ConstructorCall JL5SuperCall(Position pos, Expr outer, List args, List typeArgs);

    public JL5Call JL5Call(Position pos, Receiver target, Id name, List args, List typeArgs);

    public JL5New JL5New(Position pos, Expr qualifier, TypeNode tn, List arguments, ClassBody body, List typeArgs);
    
    public JL5New JL5New(Position pos, TypeNode tn, List arguments, ClassBody body, List typeArgs);


    public JL5Instanceof JL5Instanceof(Position pos, Expr expr, TypeNode tn);

    public JL5Import Import(Position pos, Import.Kind kind, QName Name);

    public JL5Import Import(Position pos, Import.Kind kind, QName Name, boolean isStatic);
    
    public JL5Catch JL5Catch(Position pos, Formal formal, Block body);

    public JL5NewArray JL5NewArray(Position pos, TypeNode baseType, List dims, int addDims, ArrayInit init);

    public JL5Switch JL5Switch(Position pos, Expr expr, List elements);

    public JL5If JL5If(Position pos, Expr cond, Stmt conseq, Stmt altern);
    public JL5Conditional JL5Conditional(Position pos, Expr cond, Expr conseq, Expr altern);
    public JL5Assert JL5Assert(Position pos, Expr cond, Expr errorMsg);
    public JL5Cast JL5Cast(Position pos, TypeNode castType, Expr expr);
    public JL5Return JL5Return(Position pos, Expr expr);
    
    public Binary.Operator getBinOpFromAssignOp(Assign.Operator op);
    public EnumDecl EnumDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType,  List interfaces, ClassBody body);
    public EnumDecl EnumDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType,  List interfaces, ClassBody body, List paramTypes);
    
}

