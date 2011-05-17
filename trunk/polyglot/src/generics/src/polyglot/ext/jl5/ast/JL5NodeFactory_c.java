package polyglot.ext.jl5.ast;

import java.util.LinkedList;
import java.util.List;

import polyglot.ast.AmbQualifierNode;
import polyglot.ast.AmbTypeNode;
import polyglot.ast.ArrayInit;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.ClassBody;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Disamb;
import polyglot.ast.Expr;
import polyglot.ast.FlagsNode;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.Import;
import polyglot.ast.New;
import polyglot.ast.NodeFactory_c;
import polyglot.ast.Prefix;
import polyglot.ast.Receiver;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.types.ClassDef.Kind;
import polyglot.types.Flags;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.Position;
import polyglot.util.TypedList;

/**
 * NodeFactory for jl5 extension.
 */
public class JL5NodeFactory_c extends NodeFactory_c implements JL5NodeFactory {
    // TODO:  Implement factory methods for new AST nodes.
    public ExtendedFor ExtendedFor(Position pos, List varDecls, Expr expr, Stmt stmt){
        ExtendedFor n = new ExtendedFor_c(pos, varDecls, expr, stmt);
        return n;
    }
    public EnumConstantDecl EnumConstantDecl(Position pos, FlagAnnotations flags, Id name, List args, ClassBody body){
    	//CHECK enum constant to be implemented
    	//        EnumConstantDecl n = new EnumConstantDecl_c(pos, flags, name, args, body);
    	//        return n;
    	assert false;
    	return null;
    }
    public EnumConstantDecl EnumConstantDecl(Position pos, FlagAnnotations flags, Id name, List args){
    	//CHECK enum constant to be implemented
    	//      EnumConstantDecl n = new EnumConstantDecl_c(pos, flags, name, args, null);
    	//        return n;
    	assert false;
    	return null;
    }
    
    public EnumDecl EnumDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType,  List interfaces, ClassBody body){
    	EnumDecl n = new EnumDecl_c(pos, flags, name, superType, interfaces, body);
        return n;
    }
    
    public EnumDecl EnumDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType,  List interfaces, ClassBody body, List paramTypes) {
    	EnumDecl n = new EnumDecl_c(pos, flags, name, superType, interfaces, body, paramTypes);
    	return n;
    }
    
    public JL5ClassDecl JL5ClassDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType,  List interfaces, ClassBody body, List paramTypes ){
        JL5ClassDecl n;
        if (paramTypes == null){
            n = new JL5ClassDecl_c(pos, flags, name, superType, interfaces, body);
        }
        else {
            n = new JL5ClassDecl_c(pos, flags, name, superType, interfaces, body, paramTypes);
        }
        return n;
    }
    public JL5ClassBody JL5ClassBody(Position pos, List members){
        JL5ClassBody n = new JL5ClassBody_c(pos, members);
        return n;
    }

    @Override
    public ConstructorDecl ConstructorDecl(Position pos, FlagsNode flags, Id name,
            List<Formal> formals, List<TypeNode> throwTypes,
            Block body) {
    	// JL5ConstructorDecl is a bit different as we use the same object to store annotations and flags
    	// but we want to be able to call the regular api to reuse super implementation 
        FlagAnnotations fl = new FlagAnnotations();
        fl.classicFlags(flags);
    	return JL5ConstructorDecl(pos, fl, name, formals, throwTypes, body);
    }

    public JL5ConstructorDecl JL5ConstructorDecl(Position pos, FlagAnnotations flags, Id name, List formals, List throwTypes, Block body){
    	return new JL5ConstructorDecl_c(pos, flags, name, formals, throwTypes, body);
    }

    public JL5ConstructorDecl JL5ConstructorDecl(Position pos, FlagAnnotations flags, Id name, List formals, List throwTypes, Block body, List typeParams){
    	assert (typeParams != null);
    	return new JL5ConstructorDecl_c(pos, flags, name, formals, throwTypes, body, typeParams);
    }

    public JL5Field Field(Position pos, Receiver target, Id name){
        JL5Field n = new JL5Field_c(pos, target, name);
        return n;
    }

    public JL5Case JL5Case(Position pos, Expr expr){
        JL5Case n = new JL5Case_c(pos, expr);
        return n;
    }
    
    public Disamb disamb(){
        return new JL5Disamb_c();
    }
    
    public JL5MethodDecl JL5MethodDecl(Position pos, FlagAnnotations flags, TypeNode returnType, Id name, List formals, List throwTypes, Block body, List typeParams){
        JL5MethodDecl n;
        if (typeParams == null){
            n = new JL5MethodDecl_c(pos, flags, returnType, name, formals, throwTypes, body);
        }
        else {
            n = new JL5MethodDecl_c(pos, flags, returnType, name, formals, throwTypes, body, typeParams);
        }
        return n;
    }
    
    public AnnotationElemDecl AnnotationElemDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr def){
        AnnotationElemDecl n = new AnnotationElemDecl_c(pos, flags, type, name, def);
        return n;
    }
    
    public NormalAnnotationElem NormalAnnotationElem(Position pos, TypeNode name, List elements){
        NormalAnnotationElem n = new NormalAnnotationElem_c(pos, name, elements);
        return n;
    }
    
    public MarkerAnnotationElem MarkerAnnotationElem(Position pos, TypeNode name){
        MarkerAnnotationElem n = new MarkerAnnotationElem_c(pos, name);
        return n;
    }
    
    public SingleElementAnnotationElem SingleElementAnnotationElem(Position pos, TypeNode name, Expr value){
        List l = new TypedList(new LinkedList(), ElementValuePair.class, false);
        l.add(ElementValuePair(pos, Id(value.position(), "value"), value));
        SingleElementAnnotationElem n = new SingleElementAnnotationElem_c(pos, name, l);
        return n;
    }

   
    public ElementValuePair ElementValuePair(Position pos, Id name, Expr value){
        ElementValuePair n = new ElementValuePair_c(pos, name, value);
        return n;
    }
    
    public JL5FieldDecl JL5FieldDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr init){
        JL5FieldDecl n = new JL5FieldDecl_c(pos, flags, type, name, init);
        return n;
    }
    public JL5Formal JL5Formal(Position pos, FlagAnnotations flags, TypeNode type, Id name){
        JL5Formal n = new JL5Formal_c(pos, flags, type, name);
        return n;
    }
    public JL5LocalDecl JL5LocalDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr init){
        JL5LocalDecl n = new JL5LocalDecl_c(pos, flags, type, name, init);
        return n;
    }
    
    public JL5PackageNode JL5PackageNode(Position pos, Ref<? extends Package> p) {
    	JL5PackageNode n = new JL5PackageNode_c(pos, new FlagAnnotations(FlagsNode(pos,Flags.NONE)), p);
        return n;    	
    }
    
    public JL5PackageNode JL5PackageNode(Position pos, FlagAnnotations fl, Ref<? extends Package> p) {
    	JL5PackageNode n = new JL5PackageNode_c(pos, fl, p);
        return n;
    }

    @Override
    public polyglot.ast.ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base) {
    	return this.ArrayTypeNode(pos, base, false);
    }

    public polyglot.ast.ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base, boolean varargs) {
    	return new JL5ArrayTypeNode_c(pos, base, varargs);
    }
    
    public ParamTypeNode ParamTypeNode(Position pos, List bounds, String id){
        ParamTypeNode n = new ParamTypeNode_c(pos, bounds, id);
        return n;
    }
    
    public BoundedTypeNode BoundedTypeNode(Position pos, Kind kind, TypeNode bound){
        BoundedTypeNode n = new BoundedTypeNode_c(pos, kind, bound);
        return n;
    }

    public AmbQualifierNode JL5AmbQualifierNode(Position pos, Prefix qual, Id name, List args){
        AmbQualifierNode n = new JL5AmbQualifierNode_c(pos, qual, name, args);
        return n;
    }
    
    public AmbTypeNode AmbTypeNode(Position pos, Prefix qualifier, Id name) {
	return JL5AmbTypeNode(pos, qualifier, name, new LinkedList());
    }

    public AmbTypeNode JL5AmbTypeNode(Position pos, Prefix qual, Id name, List args){
        AmbTypeNode n = new JL5AmbTypeNode_c(pos, qual, name, args);
        return n;
    }
    
    public ConstructorCall JL5ThisCall(Position pos, List args, List typeArgs){
        return JL5ThisCall(pos, null, args, typeArgs);
    }

    public ConstructorCall JL5ThisCall(Position pos, Expr outer, List args, List typeArgs){
        ConstructorCall n = new JL5ConstructorCall_c(pos, ConstructorCall.THIS, outer, args, typeArgs);
        return n;
    }

    public ConstructorCall JL5SuperCall(Position pos, List args, List typeArgs){
        return JL5SuperCall(pos, null, args, typeArgs);
    }

    public ConstructorCall JL5SuperCall(Position pos, Expr outer, List args, List typeArgs){
        ConstructorCall n = new JL5ConstructorCall_c(pos, ConstructorCall.SUPER, outer, args, typeArgs);
        return n;
    }

    public JL5Call JL5Call(Position pos, Receiver target, Id name, List args, List typeArgs){
        JL5Call n = new JL5Call_c(pos, target, name, args, typeArgs);
        return n;
    }

    public JL5ClassLit ClassLit(Position pos, TypeNode typenode) {
	return new JL5ClassLit_c(pos, typenode);
    }

    public JL5New JL5New(Position pos, Expr qualifier, TypeNode tn, List arguments, ClassBody body, List typeArgs){
        JL5New n = new JL5New_c(pos, qualifier, tn, arguments, body, typeArgs);
        return n;
    }
    
    public JL5New JL5New(Position pos, TypeNode tn, List arguments, ClassBody body, List typeArgs){
        JL5New n = new JL5New_c(pos, null, tn, arguments, body, typeArgs);
        return n;
    }

    public JL5Instanceof JL5Instanceof(Position pos, Expr expr, TypeNode tn){
        JL5Instanceof n = new JL5Instanceof_c(pos, expr, tn);
        return n;
    }

    public JL5Import Import(Position pos, Import.Kind kind, QName name){
        JL5Import n = new JL5Import_c(pos, kind, name, false);
        return n;
    }

    public JL5Import Import(Position pos, Import.Kind kind, QName name, boolean isStatic){
        JL5Import n = new JL5Import_c(pos, kind, name, isStatic);
        return n;
    }

    public JL5CanonicalTypeNode CanonicalTypeNode(Position pos, Type type) {
        JL5CanonicalTypeNode n = new JL5CanonicalTypeNode_c(pos, Types.<Type>ref(type));
        return n;
    }
    public JL5Catch JL5Catch(Position pos, Formal formal, Block body){
        JL5Catch n = new JL5Catch_c(pos, formal, body);
        return n;
    }
    public JL5NewArray JL5NewArray(Position pos, TypeNode baseType, List dims, int addDims, ArrayInit init){
        JL5NewArray n = new JL5NewArray_c(pos, baseType, dims, addDims, init);
        return n;
    }
    public JL5Switch JL5Switch(Position pos, Expr expr, List elements){
        JL5Switch n = new JL5Switch_c(pos, expr, elements);
        return n;
    }
    public JL5If JL5If(Position pos, Expr cond, Stmt conseq, Stmt altern){
        JL5If n = new JL5If_c(pos, cond, conseq, altern);
        return n;
    }
    public JL5Conditional JL5Conditional(Position pos, Expr cond, Expr conseq, Expr altern){
        JL5Conditional n = new JL5Conditional_c(pos, cond, conseq, altern);
        return n;
    }
    
    public JL5Assert JL5Assert(Position pos, Expr cond, Expr errorMsg){
        JL5Assert n = new JL5Assert_c(pos, cond, errorMsg);
        return n;
    }
    public JL5Cast JL5Cast(Position pos, TypeNode castType, Expr expr){
        JL5Cast n = new JL5Cast_c(pos, castType, expr);
        return n;
    }

    public JL5Return JL5Return(Position pos, Expr expr){
        JL5Return n = new JL5Return_c(pos, expr);
        return n;
    }

    public Binary.Operator getBinOpFromAssignOp(Assign.Operator op){
        if (op == Assign.ADD_ASSIGN) return Binary.ADD;
        if (op == Assign.BIT_OR_ASSIGN) return Binary.BIT_OR;
        if (op == Assign.BIT_AND_ASSIGN) return Binary.BIT_AND;
        if (op == Assign.BIT_XOR_ASSIGN) return Binary.BIT_XOR;
        if (op == Assign.DIV_ASSIGN) return Binary.DIV;
        if (op == Assign.MOD_ASSIGN) return Binary.MOD;
        if (op == Assign.MUL_ASSIGN) return Binary.MUL;
        if (op == Assign.SHL_ASSIGN) return Binary.SHL;
        if (op == Assign.SHR_ASSIGN) return Binary.SHR;
        if (op == Assign.SUB_ASSIGN) return Binary.SUB;
        if (op == Assign.USHR_ASSIGN) return Binary.USHR;
        else throw new RuntimeException("Unknown op: "+op); 
    }
    
    @Override
    public JL5ConstructorCall ConstructorCall(Position pos, ConstructorCall.Kind kind, Expr outer, List args) {
        return new JL5ConstructorCall_c(pos, kind, outer, args, null);
    }
    @Override
    public New New(Position pos, Expr outer, TypeNode objectType, List args, ClassBody body) {
        return new JL5New_c(pos, outer, objectType, args, body, null);
    }
    
    // TODO:  Override factory methods for overriden AST nodes.
    // TODO:  Override factory methods for AST nodes with new extension nodes.
}