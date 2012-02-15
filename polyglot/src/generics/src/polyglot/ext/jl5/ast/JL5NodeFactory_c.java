package polyglot.ext.jl5.ast;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.AmbQualifierNode;
import polyglot.ast.AmbTypeNode;
import polyglot.ast.ArrayInit;
import polyglot.ast.ArrayTypeNode;
import polyglot.ast.Assert;
import polyglot.ast.Assign;
import polyglot.ast.Binary;
import polyglot.ast.Block;
import polyglot.ast.Call;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Case;
import polyglot.ast.Cast;
import polyglot.ast.Catch;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassDecl;
import polyglot.ast.ClassLit;
import polyglot.ast.ClassMember;
import polyglot.ast.Conditional;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.DelFactory;
import polyglot.ast.Disamb;
import polyglot.ast.Expr;
import polyglot.ast.ExtFactory;
import polyglot.ast.Field;
import polyglot.ast.FieldDecl;
import polyglot.ast.FlagsNode;
import polyglot.ast.Formal;
import polyglot.ast.Id;
import polyglot.ast.If;
import polyglot.ast.Import;
import polyglot.ast.Instanceof;
import polyglot.ast.LocalDecl;
import polyglot.ast.MethodDecl;
import polyglot.ast.New;
import polyglot.ast.NewArray;
import polyglot.ast.NodeFactory_c;
import polyglot.ast.PackageNode;
import polyglot.ast.Prefix;
import polyglot.ast.Receiver;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.ast.Switch;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.FlagAnnotations;
import polyglot.types.ClassDef.Kind;
import polyglot.types.Flags;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;

/**
 * NodeFactory for jl5 extension.
 */
public class JL5NodeFactory_c extends NodeFactory_c implements JL5NodeFactory {
	public JL5NodeFactory_c() {
		super();
	}

	public JL5NodeFactory_c(ExtFactory extFactory) {
		super(extFactory);
	}

	public JL5NodeFactory_c(ExtFactory extFactory, DelFactory delFactory) {
		super(extFactory, delFactory);
	}

	@Override
	public AmbQualifierNode AmbQualifierNode(Position pos, Prefix qualifier, Id name) {
		AmbQualifierNode n = new JL5AmbQualifierNode_c(pos, qualifier, name, Collections.EMPTY_LIST);
		n = (AmbQualifierNode)n.ext(extFactory().extLocalAssign());
		n = (AmbQualifierNode)n.del(delFactory().delLocalAssign());
		return n;
	}

	public AmbQualifierNode AmbQualifierNode(Position pos, Prefix qual, Id name, List args){
		AmbQualifierNode n = new JL5AmbQualifierNode_c(pos, qual, name, args);
		n = (AmbQualifierNode)n.ext(extFactory().extLocalAssign());
		n = (AmbQualifierNode)n.del(delFactory().delLocalAssign());
		return n;
	}

    public AmbTypeNode AmbTypeNode(Position pos, Prefix qualifier, Id name) {
		AmbTypeNode n = new JL5AmbTypeNode_c(pos, qualifier, name);
		n = (AmbTypeNode)n.ext(extFactory().extLocalAssign());
		n = (AmbTypeNode)n.del(delFactory().delLocalAssign());
		return n;
    }
    
	public AmbTypeNode AmbTypeNode(Position pos, Prefix qualifier, Id name, List args){
		AmbTypeNode n = new JL5AmbTypeNode_c(pos, qualifier, name, args);
		n = (AmbTypeNode)n.ext(extFactory().extLocalAssign());
		n = (AmbTypeNode)n.del(delFactory().delLocalAssign());
		return n;
	}

	public AnnotationElemDecl AnnotationElemDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr def){
		AnnotationElemDecl n = new AnnotationElemDecl_c(pos, flags, type, name, def);
		n = (AnnotationElemDecl)n.ext(extFactory().extLocalAssign());
		n = (AnnotationElemDecl)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public polyglot.ast.ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base) {
		return this.ArrayTypeNode(pos, base, false);
	}
	
	public ArrayTypeNode ArrayTypeNode(Position pos, TypeNode base, boolean varargs) {
		ArrayTypeNode n = new JL5ArrayTypeNode_c(pos, base, varargs);
		n = (ArrayTypeNode)n.ext(extFactory().extLocalAssign());
		n = (ArrayTypeNode)n.del(delFactory().delLocalAssign());
		return n;
	}
	
	public Assert Assert(Position pos, Expr cond, Expr errorMsg){
		Assert n = new JL5Assert_c(pos, cond, errorMsg);
		n = (Assert)n.ext(extFactory().extLocalAssign());
		n = (Assert)n.del(delFactory().delLocalAssign());
		return n;
	}

	public BoundedTypeNode BoundedTypeNode(Position pos, Kind kind, TypeNode bound){
		BoundedTypeNode n = new BoundedTypeNode_c(pos, kind, bound);
		n = (BoundedTypeNode)n.ext(extFactory().extLocalAssign());
		n = (BoundedTypeNode)n.del(delFactory().delLocalAssign());
		return n;
	}

	public Call Call(Position pos, Receiver target, Id name, List<Expr> args, List<TypeNode> typeArgs) {
		Call n = new JL5Call_c(pos, target, name, args, typeArgs);
		n = (Call)n.ext(extFactory().extLocalAssign());
		n = (Call)n.del(delFactory().delLocalAssign());
		return n;
	}

	public Call Call(Position pos, Receiver target, Id name, List<Expr> args) {
		return Call(pos, target, name, CollectionUtil.nonNullList(args), Collections.EMPTY_LIST);
	}

	@Override
    public CanonicalTypeNode CanonicalTypeNode(Position pos, Ref<? extends Type> type) {
		CanonicalTypeNode n = new JL5CanonicalTypeNode_c(pos, type);
		n = (CanonicalTypeNode)n.ext(extFactory().extLocalAssign());
		n = (CanonicalTypeNode)n.del(delFactory().delLocalAssign());
		return n;
    }

	public CanonicalTypeNode CanonicalTypeNode(Position pos, Type type) {
		return CanonicalTypeNode(pos, Types.<Type>ref(type));
	}

	@Override
	public Case Case(Position pos, Expr expr){
		Case n = new JL5Case_c(pos, expr);
		n = (Case)n.ext(extFactory().extLocalAssign());
		n = (Case)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override 
	public Cast Cast(Position pos, TypeNode castType, Expr expr){
		Cast n = new JL5Cast_c(pos, castType, expr);
		n = (Cast)n.ext(extFactory().extLocalAssign());
		n = (Cast)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public Catch Catch(Position pos, Formal formal, Block body){
		Catch n = new JL5Catch_c(pos, formal, body);
		n = (Catch)n.ext(extFactory().extLocalAssign());
		n = (Catch)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public ClassBody ClassBody(Position pos, List<ClassMember> members) {
		ClassBody n = new JL5ClassBody_c(pos, members);
		n = (ClassBody)n.ext(extFactory().extLocalAssign());
		n = (ClassBody)n.del(delFactory().delLocalAssign());
		return n;
	}

	public ClassDecl ClassDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType,  
			List<TypeNode> interfaces, ClassBody body, List paramTypes){
		ClassDecl n;
		if (paramTypes == null) {
			n = new JL5ClassDecl_c(pos, flags, name, superType, interfaces, body);
		} else {
			n = new JL5ClassDecl_c(pos, flags, name, superType, interfaces, body, paramTypes);
		}
		n = (ClassDecl)n.ext(extFactory().extLocalAssign());
		n = (ClassDecl)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public ClassDecl ClassDecl(Position pos, FlagsNode flags, Id name, TypeNode superClass, List<TypeNode> interfaces, ClassBody body) {
		return ClassDecl(pos, new FlagAnnotations(flags, null), name, superClass, interfaces, body, null);
	}

	@Override
	public ClassLit ClassLit(Position pos, TypeNode typenode) {
		ClassLit n = new JL5ClassLit_c(pos, typenode);
		n = (ClassLit)n.ext(extFactory().extLocalAssign());
		n = (ClassLit)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public Conditional Conditional(Position pos, Expr cond, Expr conseq, Expr altern){
		Conditional n = new JL5Conditional_c(pos, cond, conseq, altern);
		n = (Conditional)n.ext(extFactory().extLocalAssign());
		n = (Conditional)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public ConstructorCall ConstructorCall(Position pos, ConstructorCall.Kind kind, Expr outer, List<Expr> args) {
		return ConstructorCall(pos, kind, outer, args, null);
	}

	public ConstructorCall ConstructorCall(Position pos, ConstructorCall.Kind kind, Expr outer, List<Expr> args, List<TypeNode> typeArgs) {
		ConstructorCall n = new JL5ConstructorCall_c(pos, kind, outer, args, typeArgs);
		n = (ConstructorCall)n.ext(extFactory().extLocalAssign());
		n = (ConstructorCall)n.del(delFactory().delLocalAssign());
		return n;
	}

	public ConstructorDecl ConstructorDecl(Position pos, FlagAnnotations flags, Id name, 
			List<Formal> formals, List<TypeNode> throwTypes, Block body) {
		ConstructorDecl n = new JL5ConstructorDecl_c(pos, flags, name, formals, throwTypes, body);
		n = (ConstructorDecl)n.ext(extFactory().extLocalAssign());
		n = (ConstructorDecl)n.del(delFactory().delLocalAssign());
		return n;
	}

	public ConstructorDecl ConstructorDecl(Position pos, FlagAnnotations flags, Id name, 
			List<Formal> formals, List<TypeNode> throwTypes, Block body, List typeParams) {
		assert (typeParams != null);
		ConstructorDecl n = new JL5ConstructorDecl_c(pos, flags, name, formals, throwTypes, body, typeParams);
		n = (ConstructorDecl)n.ext(extFactory().extLocalAssign());
		n = (ConstructorDecl)n.del(delFactory().delLocalAssign());
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
		return ConstructorDecl(pos, fl, name, formals, throwTypes, body);
	}

	@Override
	public Disamb disamb(){
		return new JL5Disamb_c();
	}

	public ElementValuePair ElementValuePair(Position pos, Id name, Expr value){
		ElementValuePair n = new ElementValuePair_c(pos, name, value);
		n = (ElementValuePair)n.ext(extFactory().extLocalAssign());
		n = (ElementValuePair)n.del(delFactory().delLocalAssign());
		return n;
	}

	public EnumConstantDecl EnumConstantDecl(Position pos, FlagAnnotations flags, Id name, List args){
		//CHECK enum constant to be implemented
		//      EnumConstantDecl n = new EnumConstantDecl_c(pos, flags, name, args, null);
		//        return n;
		assert false;
		return null;
	}

	public EnumConstantDecl EnumConstantDecl(Position pos, FlagAnnotations flags, Id name, List args, ClassBody body){
		//CHECK enum constant to be implemented
		//        EnumConstantDecl n = new EnumConstantDecl_c(pos, flags, name, args, body);
		//        return n;
		assert false;
		return null;
	}

	public EnumDecl EnumDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType,  List interfaces, ClassBody body){
		EnumDecl n = new EnumDecl_c(pos, flags, name, superType, interfaces, body);
		n = (EnumDecl)n.ext(extFactory().extLocalAssign());
		n = (EnumDecl)n.del(delFactory().delLocalAssign());
		return n;
	}

	public EnumDecl EnumDecl(Position pos, FlagAnnotations flags, Id name, TypeNode superType,  List interfaces, ClassBody body, List paramTypes) {
		EnumDecl n = new EnumDecl_c(pos, flags, name, superType, interfaces, body, paramTypes);
		n = (EnumDecl)n.ext(extFactory().extLocalAssign());
		n = (EnumDecl)n.del(delFactory().delLocalAssign());
		return n;
	}

	public ExtendedFor ExtendedFor(Position pos, List varDecls, Expr expr, Stmt stmt){
		ExtendedFor n = new ExtendedFor_c(pos, varDecls, expr, stmt);
		n = (ExtendedFor)n.ext(extFactory().extLocalAssign());
		n = (ExtendedFor)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public Field Field(Position pos, Receiver target, Id name) {
		Field n = new JL5Field_c(pos, target, name);
		n = (Field)n.ext(extFactory().extLocalAssign());
		n = (Field)n.del(delFactory().delLocalAssign());
		return n;
	}

	public FieldDecl FieldDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr init){
		FieldDecl n = new JL5FieldDecl_c(pos, flags, type, name, init);
		n = (FieldDecl)n.ext(extFactory().extLocalAssign());
		n = (FieldDecl)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
    public FieldDecl FieldDecl(Position pos, FlagsNode flags, TypeNode type, Id name, Expr init) {
    	return FieldDecl(pos, new FlagAnnotations(flags, null), type, name, init);
    }

	public Formal Formal(Position pos, FlagAnnotations flags, TypeNode type, Id name){
		Formal n = new JL5Formal_c(pos, flags, type, name);
		n = (Formal)n.ext(extFactory().extLocalAssign());
		n = (Formal)n.del(delFactory().delLocalAssign());
		return n;
	}

	/**
	 * All Formal must be JL5Formal
	 */
	@Override
	public Formal Formal(Position pos, FlagsNode flags, TypeNode type, Id name) {
		Formal n = Formal(pos, new FlagAnnotations(flags), type, name);
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
	public If If(Position pos, Expr cond, Stmt conseq, Stmt altern){
		If n = new JL5If_c(pos, cond, conseq, altern);
		n = (If)n.ext(extFactory().extLocalAssign());
		n = (If)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public Import Import(Position pos, Import.Kind kind, QName name){
		Import n = new JL5Import_c(pos, kind, name);
		n = (Import)n.ext(extFactory().extLocalAssign());
		n = (Import)n.del(delFactory().delLocalAssign());
		return n;
	}
    
	@Override
	public Instanceof Instanceof(Position pos, Expr expr, TypeNode tn){
		Instanceof n = new JL5Instanceof_c(pos, expr, tn);
		n = (Instanceof)n.ext(extFactory().extLocalAssign());
		n = (Instanceof)n.del(delFactory().delLocalAssign());
		return n;
	}

	public LocalDecl LocalDecl(Position pos, FlagAnnotations flags, TypeNode type, Id name, Expr init){
		LocalDecl n = new JL5LocalDecl_c(pos, flags, type, name, init);
		n = (LocalDecl)n.ext(extFactory().extLocalAssign());
		n = (LocalDecl)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
    public LocalDecl LocalDecl(Position pos, FlagsNode flags, TypeNode type, Id name, Expr init) {
		return LocalDecl(pos, new FlagAnnotations(flags, null), type, name, init);
    }

	public MarkerAnnotationElem MarkerAnnotationElem(Position pos, TypeNode name){
		MarkerAnnotationElem n = new MarkerAnnotationElem_c(pos, name);
		n = (MarkerAnnotationElem)n.ext(extFactory().extLocalAssign());
		n = (MarkerAnnotationElem)n.del(delFactory().delLocalAssign());
		return n;
	}

	public MethodDecl MethodDecl(Position pos, FlagAnnotations flags, TypeNode returnType, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body, List typeParams) {
		MethodDecl n;
		if (typeParams == null){
			n = new JL5MethodDecl_c(pos, flags, returnType, name, formals, throwTypes, body);
		}
		else {
			n = new JL5MethodDecl_c(pos, flags, returnType, name, formals, throwTypes, body, typeParams);
		}
		n = (MethodDecl)n.ext(extFactory().extLocalAssign());
		n = (MethodDecl)n.del(delFactory().delLocalAssign());
		return n;
	}
	
	@Override
    public MethodDecl MethodDecl(Position pos, FlagsNode flags, TypeNode returnType, Id name, List<Formal> formals, List<TypeNode> throwTypes, Block body) {
		MethodDecl n = MethodDecl(pos, new FlagAnnotations(flags, null), returnType, name, formals, throwTypes, body, Collections.EMPTY_LIST);
		n = (MethodDecl)n.ext(extFactory().extLocalAssign());
		n = (MethodDecl)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public New New(Position pos, Expr qualifier, TypeNode tn, List arguments, ClassBody body) {
		return New(pos, qualifier, tn, arguments, body, Collections.EMPTY_LIST);
	}

	public New New(Position pos, Expr qualifier, TypeNode tn, List arguments, ClassBody body, List<TypeNode> typeArgs){
		New n = new JL5New_c(pos, qualifier, tn, arguments, body, typeArgs);
		n = (New)n.ext(extFactory().extLocalAssign());
		n = (New)n.del(delFactory().delLocalAssign());
		return n;
	}

	public New New(Position pos, TypeNode tn, List arguments, ClassBody body, List<TypeNode> typeArgs){
		New n = new JL5New_c(pos, null, tn, arguments, body, typeArgs);
		n = (New)n.ext(extFactory().extLocalAssign());
		n = (New)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public NewArray NewArray(Position pos, TypeNode baseType, List dims, int addDims, ArrayInit init){
		NewArray n = new JL5NewArray_c(pos, baseType, dims, addDims, init);
		n = (NewArray)n.ext(extFactory().extLocalAssign());
		n = (NewArray)n.del(delFactory().delLocalAssign());
		return n;
	}
	
	public NormalAnnotationElem NormalAnnotationElem(Position pos, TypeNode name, List elements){
		NormalAnnotationElem n = new NormalAnnotationElem_c(pos, name, elements);
		n = (NormalAnnotationElem)n.ext(extFactory().extLocalAssign());
		n = (NormalAnnotationElem)n.del(delFactory().delLocalAssign());
		return n;
	}
	
	public PackageNode PackageNode(Position pos, FlagAnnotations fl, Ref<? extends Package> p) {
		PackageNode n = new JL5PackageNode_c(pos, fl, p);
		n = (PackageNode)n.ext(extFactory().extLocalAssign());
		n = (PackageNode)n.del(delFactory().delLocalAssign());
		return n;
	}
	
	@Override
	public PackageNode PackageNode(Position pos, Ref<? extends Package> p) {
		PackageNode n = new JL5PackageNode_c(pos, new FlagAnnotations(FlagsNode(pos,Flags.NONE)), p);
		n = (PackageNode)n.ext(extFactory().extLocalAssign());
		n = (PackageNode)n.del(delFactory().delLocalAssign());
		return n;
	}
	
	public ParamTypeNode ParamTypeNode(Position pos, List bounds, String id){
		ParamTypeNode n = new ParamTypeNode_c(pos, bounds, id);
		n = (ParamTypeNode)n.ext(extFactory().extLocalAssign());
		n = (ParamTypeNode)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public Return Return(Position pos, Expr expr){
		Return n = new JL5Return_c(pos, expr);
		n = (Return)n.ext(extFactory().extLocalAssign());
		n = (Return)n.del(delFactory().delLocalAssign());
		return n;
	}
	
	public SingleElementAnnotationElem SingleElementAnnotationElem(Position pos, TypeNode name, Expr value){
		List l = new TypedList(new LinkedList(), ElementValuePair.class, false);
		l.add(ElementValuePair(pos, Id(value.position(), "value"), value));
		SingleElementAnnotationElem n = new SingleElementAnnotationElem_c(pos, name, l);
		n = (SingleElementAnnotationElem)n.ext(extFactory().extLocalAssign());
		n = (SingleElementAnnotationElem)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public ConstructorCall SuperCall(Position pos, Expr outer, List<Expr> args, List<TypeNode> typeArgs) {
		return ConstructorCall(pos, ConstructorCall.SUPER, outer, args, typeArgs);
	}

	@Override
	public ConstructorCall SuperCall(Position pos, List<Expr> args, List<TypeNode> typeArgs) {
		return ConstructorCall(pos, ConstructorCall.SUPER, null, args, typeArgs);
	}

	@Override
	public Switch Switch(Position pos, Expr expr, List elements){
		Switch n = new JL5Switch_c(pos, expr, elements);
		n = (Switch)n.ext(extFactory().extLocalAssign());
		n = (Switch)n.del(delFactory().delLocalAssign());
		return n;
	}

	@Override
	public ConstructorCall ThisCall(Position pos, Expr outer, List<Expr> args, List<TypeNode> typeArgs) {
		return ConstructorCall(pos, ConstructorCall.THIS, outer, args, typeArgs);
	}

	@Override
	public ConstructorCall ThisCall(Position pos, List<Expr> args, List<TypeNode> typeArgs) {
		return ConstructorCall(pos, ConstructorCall.THIS, null, args, typeArgs);
	}
}