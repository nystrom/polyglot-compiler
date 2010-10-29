package polyglot.dispatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import polyglot.ast.AmbExpr_c;
import polyglot.ast.AmbQualifierNode_c;
import polyglot.ast.AmbReceiver_c;
import polyglot.ast.AmbTypeNode;
import polyglot.ast.AmbTypeNode_c;
import polyglot.ast.ArrayAccess_c;
import polyglot.ast.ArrayInit;
import polyglot.ast.ArrayInit_c;
import polyglot.ast.ArrayTypeNode_c;
import polyglot.ast.Assert_c;
import polyglot.ast.Assign;
import polyglot.ast.Assign_c;
import polyglot.ast.Binary;
import polyglot.ast.Binary_c;
import polyglot.ast.Block_c;
import polyglot.ast.BooleanLit_c;
import polyglot.ast.Branch_c;
import polyglot.ast.Call;
import polyglot.ast.Call_c;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.CanonicalTypeNode_c;
import polyglot.ast.Case;
import polyglot.ast.Case_c;
import polyglot.ast.Cast;
import polyglot.ast.Cast_c;
import polyglot.ast.Catch_c;
import polyglot.ast.CharLit_c;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassBody_c;
import polyglot.ast.ClassDecl_c;
import polyglot.ast.ClassLit_c;
import polyglot.ast.Conditional_c;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorCall_c;
import polyglot.ast.ConstructorDecl_c;
import polyglot.ast.Do_c;
import polyglot.ast.Empty_c;
import polyglot.ast.Eval_c;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.FieldDecl_c;
import polyglot.ast.Field_c;
import polyglot.ast.FlagsNode;
import polyglot.ast.FlagsNode_c;
import polyglot.ast.FloatLit;
import polyglot.ast.FloatLit_c;
import polyglot.ast.ForInit;
import polyglot.ast.For_c;
import polyglot.ast.Formal_c;
import polyglot.ast.Id;
import polyglot.ast.Id_c;
import polyglot.ast.If_c;
import polyglot.ast.Import;
import polyglot.ast.Import_c;
import polyglot.ast.Initializer_c;
import polyglot.ast.Instanceof_c;
import polyglot.ast.IntLit;
import polyglot.ast.IntLit_c;
import polyglot.ast.Labeled_c;
import polyglot.ast.LocalClassDecl_c;
import polyglot.ast.LocalDecl;
import polyglot.ast.LocalDecl_c;
import polyglot.ast.Local_c;
import polyglot.ast.MethodDecl_c;
import polyglot.ast.NamedVariable;
import polyglot.ast.New;
import polyglot.ast.NewArray_c;
import polyglot.ast.New_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Node_c;
import polyglot.ast.NullLit_c;
import polyglot.ast.PackageNode;
import polyglot.ast.PackageNode_c;
import polyglot.ast.ProcedureCall;
import polyglot.ast.QualifiedName;
import polyglot.ast.QualifiedName_c;
import polyglot.ast.QualifierNode;
import polyglot.ast.Receiver;
import polyglot.ast.Return_c;
import polyglot.ast.SourceFile_c;
import polyglot.ast.Special;
import polyglot.ast.Special_c;
import polyglot.ast.StringLit_c;
import polyglot.ast.SwitchBlock_c;
import polyglot.ast.SwitchElement;
import polyglot.ast.Switch_c;
import polyglot.ast.Throw_c;
import polyglot.ast.TopLevelDecl;
import polyglot.ast.Try_c;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary;
import polyglot.ast.Unary_c;
import polyglot.ast.Variable;
import polyglot.ast.While_c;
import polyglot.frontend.Job;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.CodeDef;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.ErrorType;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.FunctionDef;
import polyglot.types.InitializerDef;
import polyglot.types.LocalInstance;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Named;
import polyglot.types.NoClassException;
import polyglot.types.NoMemberException;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Qualifier;
import polyglot.types.Ref;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.UnknownType;
import polyglot.types.VarInstance;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class TypeChecker extends Visitor {
    Job job;
    TypeSystem ts;
    NodeFactory nf;

    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
    }

    @Override
    public Node accept(Node n, Object... args) {
	if (n == null)
	    return null;
	Node m = n.checked();
	assert m != null : "null checked for " + n;
	return m;
    }


    public Expr convert(Expr e, Type t, Context c) {
	if (! ts.isSubtype(e.type(), t, c)) {
	     CanonicalTypeNode tn = nf.CanonicalTypeNode(e.position(), Types.ref(t));
	     Cast cast = (Cast) nf.Cast(e.position(), tn, e).type(t);
	     return setContext(cast, c);
	}
	return e;
    }
    
    public List<Expr> convert(List<Expr> es, List<Type> ts, Context c) {
	ArrayList<Expr> es2 = new ArrayList<Expr>();
	for (int i = 0; i < es.size(); i++) {
	    es2.add(convert(es.get(i), ts.get(i), c));
	}
	return es2;
    }

    public Expr convertToString(Expr e, Context c) {
	CanonicalTypeNode tn = nf.CanonicalTypeNode(e.position(), Types.ref(ts.String()));
	Id id = nf.Id(e.position(), Name.make("valueOf"));
	Call call = nf.Call(e.position(), tn, id, e);
	call = setContext(call, c);
	return (Expr) accept(call);
    }

    public Node visit(Node_c n, Context context) throws SemanticException {
	System.out.println("missing node " + n + " instanceof " + n.getClass().getName());
	return (Node_c) acceptChildren(n);
    }

    public Node visit(NewArray_c n, Context context) throws SemanticException {
	Type baseType = n.baseType().type();
	
	if (baseType instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}
	
	List<Expr> dims = new ArrayList<Expr>();
	for (Expr expr : n.dims()) {
	    if (! ts.isImplicitCastValid(expr.type(), ts.Int(), context)) {
		throw new SemanticException("Array dimension must be an integer.", expr.position());
	    }

	    dims.add(convert(expr, ts.Int(), context));
	}

	n = (NewArray_c) n.dims(dims);

	Type type = ts.arrayOf(baseType, n.dims().size() + n.additionalDims());

	ArrayInit init = n.init();
	if (init != null) {
	    init = typeCheckElements(context, type, init);
	    n = (NewArray_c) n.init(init);
	}

	return n.type(type);
    }

    private ArrayInit typeCheckElements(Context context, Type type, ArrayInit init) throws SemanticException {
	if (!type.isArray()) {
	    throw new SemanticException("Cannot initialize " + type + " with " + init.type() + ".", init.position());
	}

	// Check if we can assign each individual element.
	Type t = type.toArray().base();

	List<Expr> es = new ArrayList<Expr>();

	for (Expr e : init.elements()) {
	    Type s = e.type();

	    if (e instanceof ArrayInit) {
		ArrayInit e2 = typeCheckElements(context, t, (ArrayInit) e);
		es.add(e2);
		continue;
	    }

	    if (! ts.isImplicitCastValid(s, t, context) &&
		    ! ts.typeEquals(s, t, context) &&
		    ! ts.numericConversionValid(t, e.constantValue(), context)) {
		throw new SemanticException("Cannot assign " + s + " to " + t + ".", e.position());
	    }

	    es.add(convert(e, t, context));
	}

	return init.elements(es);
    }

    public Node visit(Assign_c n, Context context) throws SemanticException {
	n = (Assign_c) acceptChildren(n);

	Type t = n.left().type();

	if (t == null)
	    t = ts.unknownType(n.position());

	Expr right = n.right();
	Assign.Operator op = n.operator();

	Type s = right.type();
	
	if (t instanceof ErrorType || s instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}

	if (op == Assign.ASSIGN) {
	    if (!ts.isImplicitCastValid(s, t, context) && !ts.typeEquals(s, t, context) && !ts.numericConversionValid(t, right.constantValue(), context)) {

		throw new SemanticException("Cannot assign " + s + " to " + t + ".", n.position());
	    }

	    return n.right(convert(right, t, context)).type(t);
	}

	if (op == Assign.ADD_ASSIGN) {
	    // t += s
	    if (ts.typeEquals(t, ts.String(), context) && ts.canCoerceToString(s, context)) {
		return n.right(convertToString(right, context)).type(ts.String());
	    }

	    if (t.isNumeric() && s.isNumeric()) {
		return n.right(convert(right, ts.promote(t, s), context)).type(ts.promote(t, s));
	    }

	    throw new SemanticException("The " + op + " operator must have numeric or String operands.", n.position());
	}

	if (op == Assign.SUB_ASSIGN || op == Assign.MUL_ASSIGN || op == Assign.DIV_ASSIGN || op == Assign.MOD_ASSIGN) {
	    if (t.isNumeric() && s.isNumeric()) {
		return n.right(convert(right, ts.promote(t, s), context)).type(ts.promote(t, s));
	    }

	    throw new SemanticException("The " + op + " operator must have numeric operands.", n.position());
	}

	if (op == Assign.BIT_AND_ASSIGN || op == Assign.BIT_OR_ASSIGN || op == Assign.BIT_XOR_ASSIGN) {
	    if (t.isBoolean() && s.isBoolean()) {
		return n.type(ts.Boolean());
	    }

	    if (ts.isImplicitCastValid(t, ts.Long(), context) && ts.isImplicitCastValid(s, ts.Long(), context)) {
		return n.right(convert(right, ts.promote(t, s), context)).type(ts.promote(t, s));
	    }

	    throw new SemanticException("The " + op + " operator must have integral or boolean operands.", n.position());
	}

	if (op == Assign.SHL_ASSIGN || op == Assign.SHR_ASSIGN || op == Assign.USHR_ASSIGN) {
	    if (ts.isImplicitCastValid(t, ts.Long(), context) && ts.isImplicitCastValid(s, ts.Long(), context)) {
		// Only promote the left of a shift.
		return n.right(convert(right, ts.promote(t), context)).type(ts.promote(t));
	    }

	    throw new SemanticException("The " + op + " operator must have integral operands.", n.position());
	}

	throw new InternalCompilerError("Unrecognized assignment operator " + op + ".");
    }

    public Node visit(ArrayAccess_c n, Context context) throws SemanticException {
	n = (ArrayAccess_c) acceptChildren(n);

	Type arrayType = n.array().type();
	
	if (arrayType instanceof ErrorType || n.index().type() instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}
	
	if (! arrayType.isArray()) {
	    throw new SemanticException("Subscript can only follow an array type.", n.position());
	}

	if (!ts.isImplicitCastValid(n.index().type(), ts.Int(), context)) {
	    throw new SemanticException("Array subscript must be an integer.", n.position());
	}

	return n.index(convert(n.index(), ts.Int(), context)).type(n.array().type().toArray().base());
    }

    public Node visit(ArrayInit_c n, Context context) throws SemanticException {
	n = (ArrayInit_c) acceptChildren(n);

	Type type = null;

	for (Expr e : n.elements()) {
	    if (type == null) {
		type = e.type();
	    }
	    else {
		type = ts.leastCommonAncestor(type, e.type(), context);
	    }
	}

	if (type == null) {
	    return n.type(ts.Null());
	}
	else {
	    return n.type(ts.arrayOf(type));
	}
    }

    public Node visit(Assert_c n, Context context) throws SemanticException {
	n = (Assert_c) acceptChildren(n);

	if (n.cond().type() instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}

	if (!n.cond().type().isBoolean()) {
	    throw new SemanticException("Condition of assert statement must have boolean type.", n.cond().position());
	}

	if (n.errorMessage() != null && ts.typeEquals(n.errorMessage().type(), ts.Void(), context)) {
	    throw new SemanticException("Error message in assert statement must have a value.", n.errorMessage().position());
	}

	return n;
    }

    public Node visit(Binary_c n, Context context) throws SemanticException {
	n = (Binary_c) acceptChildren(n);

	Type l = n.left().type();
	Type r = n.right().type();
	
	if (l instanceof ErrorType || r instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}

	if (n.operator() == Binary.GT || n.operator() == Binary.LT || n.operator() == Binary.GE || n.operator() == Binary.LE) {
	    if (!l.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + l + ".", n.left().position());
	    }

	    if (!r.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + r + ".", n.right().position());
	    }

	    return n.left(convert(n.left(), ts.promote(l, r), context)).right(convert(n.right(), ts.promote(l, r), context)).type(ts.Boolean());
	}

	if (n.operator() == Binary.EQ || n.operator() == Binary.NE) {
	    if (!ts.isCastValid(l, r, context) && !ts.isCastValid(r, l, context)) {
		throw new SemanticException("The " + n.operator() + " operator must have operands of similar type.", n.position());
	    }

	    if (l.isNumeric() && r.isNumeric())
		return n.left(convert(n.left(), ts.promote(l, r), context)).right(convert(n.right(), ts.promote(l, r), context)).type(ts.Boolean());

	    return n.type(ts.Boolean());
	}

	if (n.operator() == Binary.COND_OR || n.operator() == Binary.COND_AND) {
	    if (!l.isBoolean()) {
		throw new SemanticException("The " + n.operator() + " operator must have boolean operands, not type " + l + ".", n.left().position());
	    }

	    if (!r.isBoolean()) {
		throw new SemanticException("The " + n.operator() + " operator must have boolean operands, not type " + r + ".", n.right().position());
	    }

	    return n.type(ts.Boolean());
	}

	if (n.operator() == Binary.ADD) {
	    if (ts.isSubtype(l, ts.String(), context) || ts.isSubtype(r, ts.String(), context)) {
		if (!ts.canCoerceToString(r, context)) {
		    throw new SemanticException("Cannot coerce an expression of type " + r + " to a String.", n.right().position());
		}
		if (!ts.canCoerceToString(l, context)) {
		    throw new SemanticException("Cannot coerce an expression of type " + l + " to a String.", n.left().position());
		}

		return n.left(convertToString(n.left(), context)).right(convertToString(n.right(), context)).type(ts.String());
	    }
	}

	if (n.operator() == Binary.BIT_AND || n.operator() == Binary.BIT_OR || n.operator() == Binary.BIT_XOR) {
	    if (l.isBoolean() && r.isBoolean()) {
		return n.type(ts.Boolean());
	    }
	}

	if (n.operator() == Binary.ADD) {
	    if (!l.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric or String operands, not type " + l + ".", n.left()
		                            .position());
	    }

	    if (!r.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric or String operands, not type " + r + ".", n.right()
		                            .position());
	    }

	    return n.left(convert(n.left(), ts.promote(l, r), context)).right(convert(n.right(), ts.promote(l, r), context)).type(ts.promote(l, r));
	}

	if (n.operator() == Binary.BIT_AND || n.operator() == Binary.BIT_OR || n.operator() == Binary.BIT_XOR) {
	    if (!ts.isImplicitCastValid(l, ts.Long(), context)) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric or boolean operands, not type " + l + ".", n.left().position());
	    }

	    if (!ts.isImplicitCastValid(r, ts.Long(), context)) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric or boolean operands, not type " + r + ".", n.right()
		                            .position());
	    }

	    return n.left(convert(n.left(), ts.promote(l, r), context)).right(convert(n.right(), ts.promote(l, r), context)).type(ts.promote(l, r));
	}

	if (n.operator() == Binary.SUB || n.operator() == Binary.MUL || n.operator() == Binary.DIV || n.operator() == Binary.MOD) {
	    if (!l.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + l + ".", n.left().position());
	    }

	    if (!r.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + r + ".", n.right().position());
	    }

	    return n.left(convert(n.left(), ts.promote(l, r), context)).right(convert(n.right(), ts.promote(l, r), context)).type(ts.promote(l, r));
	}

	if (n.operator() == Binary.SHL || n.operator() == Binary.SHR || n.operator() == Binary.USHR) {
	    if (!ts.isImplicitCastValid(l, ts.Long(), context)) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + l + ".", n.left().position());
	    }

	    if (!ts.isImplicitCastValid(r, ts.Long(), context)) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + r + ".", n.right().position());
	    }

	    // For shift, only promote the left operand.
	    return n.left(convert(n.left(), ts.promote(l), context)).right(convert(n.right(), ts.Int(), context)).type(ts.promote(l));
	}

	return n.type(ts.promote(l, r));
    }

    public Node visit(Unary_c n, Context context) throws SemanticException {
	n = (Unary_c) acceptChildren(n);

	Type t = n.expr().type();

	if (t instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}

	if (n.operator() == Unary.POST_INC || n.operator() == Unary.POST_DEC || n.operator() == Unary.PRE_INC || n.operator() == Unary.PRE_DEC) {
	    if (!t.isNumeric()) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be numeric.", n.expr().position());
	    }
	    
	    if (!(n.expr() instanceof Variable)) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be a variable.", n.expr().position());
	    }
	    
	    
	    if (n.expr() instanceof NamedVariable) {
		if (((NamedVariable) n.expr()).varInstance().flags().isFinal()) {
		    throw new SemanticException("Operand of " + n.operator() + " operator must be a non-final variable.", n.expr().position());
		}
	    }

	    return n.type(t);
	}

	if (n.operator() == Unary.BIT_NOT) {
	    if (!ts.isImplicitCastValid(t, ts.Long(), context)) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be numeric.", n.expr().position());
	    }

	    return n.expr(convert(n.expr(), ts.promote(t), context)).type(ts.promote(t));
	}

	if (n.operator() == Unary.NEG || n.operator() == Unary.POS) {
	    if (!t.isNumeric()) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be numeric.", n.expr().position());
	    }

	    return n.expr(convert(n.expr(), ts.promote(t), context)).type(ts.promote(t));
	}

	if (n.operator() == Unary.NOT) {
	    if (!t.isBoolean()) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be boolean.", n.expr().position());
	    }

	    return n.type(t);
	}

	return n;
    }

    public Node visit(While_c n, Context context) throws SemanticException {
	n = (While_c) acceptChildren(n);

	if (!(n.cond().type() instanceof ErrorType)) {
	    if (!ts.typeEquals(n.cond().type(), ts.Boolean(), context)) {
		throw new SemanticException("Condition of while statement must have boolean type.", n.cond().position());
	    }
	}

	return n.type(ts.Void());
    }

    public Node visit(Do_c n, Context context) throws SemanticException {
	n = (Do_c) acceptChildren(n);

	if (!(n.cond().type() instanceof ErrorType)) {
	    if (!ts.typeEquals(n.cond().type(), ts.Boolean(), context)) {
		throw new SemanticException("Condition of do statement must have boolean type.", n.cond().position());
	    }
	}

	return n.type(ts.Void());
    }

    public Node visit(If_c n, Context context) throws SemanticException {
	n = (If_c) acceptChildren(n);

	if (!(n.cond().type() instanceof ErrorType)) {
	    if (!ts.typeEquals(n.cond().type(), ts.Boolean(), context)) {
		throw new SemanticException("Condition of if statement must have boolean type.", n.cond().position());
	    }
	}

	return n.type(ts.Void());
    }

    public Node visit(Empty_c n, Context context) throws SemanticException {
	n = (Empty_c) acceptChildren(n);
	return n.type(ts.Void());
    }

    public Node visit(Eval_c n, Context context) throws SemanticException {
	n = (Eval_c) acceptChildren(n);
	return n.type(ts.Void());
    }

    public Node visit(Labeled_c n, Context context) throws SemanticException {
	n = (Labeled_c) acceptChildren(n);
	return n.type(ts.Void());
    }

    public Node visit(Switch_c n, Context context) throws SemanticException {
	n = (Switch_c) acceptChildren(n);

	if (!(n.expr().type() instanceof ErrorType)) {
	    if (!ts.isImplicitCastValid(n.expr().type(), ts.Int(), context) && !ts.isImplicitCastValid(n.expr().type(), ts.Char(), context)) {
		throw new SemanticException("Switch index must be an integer.", n.position());
	    }
	}
	
	Collection<Object> labels = new HashSet<Object>();

	// Check for duplicate labels.
	for (SwitchElement s : n.elements()) {
	    if (s instanceof Case) {
		Case c = (Case) s;
		Object key;
		String str;

		if (c.isDefault()) {
		    key = "default";
		    str = "default";
		}
		else if (c.expr().isConstant()) {
		    key = Long.valueOf(c.value());
		    str = c.expr().toString() + " (" + c.value() + ")";
		}
		else {
		    continue;
		}

		if (labels.contains(key)) {
		    throw new SemanticException("Duplicate case label: " + str + ".", c.position());
		}

		labels.add(key);
	    }
	}

	n = (Switch_c) n.expr(convert(n.expr(), ts.Int(), context));
	return n.type(ts.Void());
    }

    public Node visit(Call_c n, Context context) throws SemanticException {
	n = (Call_c) acceptChildren(n);

	Context c = context;

	List<Type> argTypes = new ArrayList<Type>(n.arguments().size());

	for (Expr e : n.arguments()) {
	    if (e.type() instanceof ErrorType) {
		return n.methodInstance(errorMI(n)).type(ts.errorType(n.position()));
	    }
	    argTypes.add(e.type());
	}
	
	if (n.target() == null) {
	    return typeCheckNullTarget(n, context, argTypes);
	}

	Type targetType = n.target().type();
	if (targetType instanceof ErrorType) {
	    return n.methodInstance(errorMI(n)).type(ts.errorType(n.position()));
	}

	MethodInstance mi = ts.findMethod(targetType, ts.MethodMatcher(targetType, n.name().id(), argTypes, c));

	/*
	 * This call is in a static context if and only if the target
	 * (possibly implicit) is a type node.
	 */
	boolean staticContext = (n.target() instanceof TypeNode);

	if (staticContext && !mi.flags().isStatic()) {
	    throw new SemanticException("Cannot call non-static method " + n.name().id() + " of " + n.target().type() + " in static context.",
	                                n.position());
	}

	// If the target is super, but the method is abstract, then
	// complain.
	if (n.target() instanceof Special && ((Special) n.target()).kind() == Special.SUPER && mi.flags().isAbstract()) {
	    throw new SemanticException("Cannot call an abstract method of the super class", n.position());
	}

	Call_c call = (Call_c) n.methodInstance(mi).type(mi.returnType());

	checkCallConsistency(c, call);

	return call.arguments(convert(call.arguments(), mi.formalTypes(), context));
    }

    private MethodInstance errorMI(Call n) {
	Position pos = n.position();
	List<Ref<? extends Type>> l = new ArrayList<Ref<? extends Type>>();
	Ref<ErrorType> errorTypeRef = Types.ref(ts.errorType(pos));
	for (int i = 0; i < n.arguments().size(); i++)
	    l.add(errorTypeRef);
	MethodDef errorMD = ts.methodDef(pos, errorTypeRef, Flags.NONE, errorTypeRef, n.name().id(), l, Collections.EMPTY_LIST);
	return ts.createMethodInstance(pos, Types.ref(errorMD));
    }
    
    private ConstructorInstance errorCI(ProcedureCall n) {
	Position pos = n.position();
	List<Ref<? extends Type>> l = new ArrayList<Ref<? extends Type>>();
	Ref<ErrorType> errorTypeRef = Types.ref(ts.errorType(pos));
	for (int i = 0; i < n.arguments().size(); i++)
	    l.add(errorTypeRef);
	ConstructorDef errorCD = ts.constructorDef(pos, errorTypeRef, Flags.NONE, l, Collections.EMPTY_LIST);
	return ts.createConstructorInstance(pos, Types.ref(errorCD));
    }

    private FieldInstance errorFI(Field n) {
	Position pos = n.position();
	List<Ref<? extends Type>> l = new ArrayList<Ref<? extends Type>>();
	Ref<ErrorType> errorTypeRef = Types.ref(ts.errorType(pos));
	FieldDef errorFD = ts.fieldDef(pos, errorTypeRef, Flags.NONE, errorTypeRef, n.name().id());
	return ts.createFieldInstance(pos, Types.ref(errorFD));
    }

    private void checkCallConsistency(Context c, Call_c call) throws SemanticException {
	// If we found a method, the call must type check, so no need to check
	// the arguments here.
	if (call.isTargetImplicit()) {
	    // the target is implicit. Check that the
	    // method found in the target type is the
	    // same as the method found in the context.

	    // as exception will be thrown if no appropriate method
	    // exists. 
	    MethodInstance ctxtMI = c.findMethod(c.typeSystem().MethodMatcher(null, call.name().id(), call.methodInstance().formalTypes(), c));

	    // cannot perform this check due to the context's findMethod returning a 
	    // different method instance than the typeSystem in some situations
	    //          if (!c.typeSystem().equals(ctxtMI, mi)) {
	    //              throw new InternalCompilerError("Method call " + this + " has an " +
	    //                   "implicit target, but the name " + name + " resolves to " +
	    //                   ctxtMI + " in " + ctxtMI.container() + " instead of " + mi+ " in " + mi.container(), position());
	    //          }
	}
    }

    private Node typeCheckNullTarget(Call_c n, Context context, List<Type> argTypes) throws SemanticException {
	// the target is null, and thus implicit
	// let's find the target, using the context, and
	// set the target appropriately, and then type check
	// the result
	MethodInstance mi = context.findMethod(ts.MethodMatcher(null, n.name().id(), argTypes, context));

	Receiver r;
	if (mi.flags().isStatic()) {
	    Type container = n.findContainer(ts, mi);            
	    r = nf.CanonicalTypeNode(n.position().startOf(), container).typeRef(Types.ref(container));
	} else {
	    // The method is non-static, so we must prepend with "this", but we
	    // need to determine if the "this" should be qualified.  Get the
	    // enclosing class which brought the method into scope.  This is
	    // different from mi.container().  mi.container() returns a super type
	    // of the class we want.
	    ClassType scope = context.findMethodScope(n.name().id());

	    if (! ts.typeEquals(scope, context.currentClass(), context)) {
		r = (Special) nf.This(n.position().startOf(),
		                      nf.CanonicalTypeNode(n.position().startOf(), scope));
	    }
	    else {
		r = (Special) nf.This(n.position().startOf());
	    }
	}

	r = (Receiver) setContext(r, context);
	r = (Receiver) accept(r);

	// we call computeTypes on the reciever too.
	Call_c call = (Call_c) n.targetImplicit(true).target(r);       
	call = (Call_c)call.methodInstance(mi).type(mi.returnType());
	//        call = (Call_c) call.methodInstance(mi);
	return call;
    }

    public Node visit(Cast_c n, Context context) throws SemanticException {
	n = (Cast_c) acceptChildren(n);

	if (n.expr().type() instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}
	
	if (n.castType().type() instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}
	
	if (!ts.isCastValid(n.expr().type(), n.castType().type(), context)) {
	    throw new SemanticException("Cannot cast the expression of type \"" + n.expr().type() + "\" to type \"" + n.castType().type() + "\".",
	                                n.position());
	}

	return n.type(n.castType().type());
    }

    public Node visit(For_c n, Context context) throws SemanticException {
	n = (For_c) acceptChildren(n);

	// Check that all initializers have the same type.
	// This should be enforced by the parser, but check again here,
	// just to be sure.
	Type t = null;

	for (ForInit s : n.inits()) {
	    if (s instanceof LocalDecl) {
		LocalDecl d = (LocalDecl) s;
		Type dt = d.typeNode().type();
		
		if (dt instanceof ErrorType) {
		    return n.type(ts.Void());
		}

		if (t == null) {
		    t = dt;
		}
		else {
		    if (!t.typeEquals(dt, context)) {
			throw new InternalCompilerError("Local variable declarations in a for loop initializer must all "
			                                + "be the same type, in this case " + t + ", not " + dt + ".", d.position());
		    }
		}
	    }
	}

	if (n.cond() != null) {
	    if (n.cond().type() instanceof ErrorType) {
		return n.type(ts.Void());
	    }
	    if (!ts.isImplicitCastValid(n.cond().type(), ts.Boolean(), context)) {
	        throw new SemanticException("The condition of a for statement must have boolean type.", n.cond().position());
	    }
	}

	return n.type(ts.Void());
    }

    public Node visit(LocalDecl_c n, Context context) throws SemanticException {
	// Check if the variable is multiply defined.
	// we do it in type check enter, instead of type check since
	// we add the declaration before we enter the scope of the
	// initializer.
	Context c = context;

	LocalInstance outerLocal = null;

	try {
	    outerLocal = c.findLocal(n.localDef().name());
	}
	catch (SemanticException e) {
	    // not found, so not multiply defined
	}

	if (outerLocal != null && outerLocal.def() != n.localDef() && c.isLocal(n.localDef().name())) {
	    throw new SemanticException("Local variable \"" + n.name() + "\" multiply defined.  Previous definition at " + outerLocal.position() + ".",
					n.position());
	}
	
	n = (LocalDecl_c) acceptChildren(n);

	try {
	    ts.checkLocalFlags(n.flags().flags());
	}
	catch (SemanticException e) {
	    throw new SemanticException(e.getMessage(), n.position());
	}

	if (n.init() != null) {
	    if (n.init() instanceof ArrayInit) {
		ArrayInit init = (ArrayInit) n.init();
		init = typeCheckElements(context, n.typeNode().type(), init);
		n = (LocalDecl_c) n.init(init);
	    }
	    else {
		if (!(n.init().type() instanceof ErrorType) && !(n.typeNode().type() instanceof ErrorType)) {
		    if (!ts.isImplicitCastValid(n.init().type(), n.typeNode().type(), context) && !ts.typeEquals(n.init().type(), n.typeNode().type(), context)
			    && !ts.numericConversionValid(n.typeNode().type(), n.init().constantValue(), context)) {
			throw new SemanticException("The type of the variable initializer \"" + n.init().type() + "\" does not match that of "
			                            + "the declaration \"" + n.typeNode().type() + "\".", n.init().position());
		    }
		}
	    }
	}

	if (n.init() == null || !n.init().isConstant() || !n.localDef().flags().isFinal()) {
	    n.localDef().setNotConstant();
	}
	else {
	    n.localDef().setConstantValue(n.init().constantValue());
	}

	return n;
    }

    public Node visit(Local_c n, Context context) throws SemanticException {
	n = (Local_c) acceptChildren(n);

	Context c = context;
	LocalInstance li = c.findLocal(n.name().id());

	// if the local is defined in an outer class, then it must be final
	if (!c.isLocal(li.name())) {
	    // this local is defined in an outer class
	    if (!li.flags().isFinal()) {
		throw new SemanticException("Local variable \"" + li.name() + "\" is accessed from an inner class, and must be declared final.",
		                            n.position());
	    }
	}

	return n.localInstance(li).type(li.type());
    }

    public Node visit(Special_c n, Context context) throws SemanticException {
	n = (Special_c) acceptChildren(n);

	Context c = context;

	ClassType t = null;

	if (n.qualifier() == null) {
	    // an unqualified "this" or "super"
	    t = c.currentClass();
	}
	else {
	    if (n.qualifier().type().isClass()) {
		t = n.qualifier().type().toClass();

		if (!c.currentClass().hasEnclosingInstance(t)) {
		    throw new SemanticException("The nested class \"" + c.currentClass() + "\" does not have an enclosing instance of type \"" + t
		                                + "\".", n.qualifier().position());
		}
	    }
	    else {
		throw new SemanticException("Invalid qualifier for \"this\" or \"super\".", n.qualifier().position());
	    }
	}

	if (t == null || (c.inStaticContext() && ts.typeEquals(t, c.currentClass(), c))) {
	    // trying to access "this" or "super" from a static context.
	    throw new SemanticException("Cannot access a non-static member or refer to \"this\" or \"super\" from a static context.",
	                                n.position());
	}

	if (n.kind() == Special.THIS) {
	    return n.type(t);
	}
	else if (n.kind() == Special.SUPER) {
	    return n.type(t.superClass());
	}

	return n;
    }

    public Node visit(ConstructorCall_c n, Context context) throws SemanticException {
	n = (ConstructorCall_c) acceptChildren(n);

	ConstructorCall_c n1 = n;

	Context c = context;

	ClassType ct = c.currentClass();
	Type superType = ct.superClass();
	
	if (n.kind() == ConstructorCall.SUPER && superType instanceof ErrorType) {
	    return n1.constructorInstance(errorCI(n)).type(ts.Void());
	}


	// The qualifier specifies the enclosing instance of this inner
	// class.
	// The type of the qualifier must be the outer class of this
	// inner class or one of its super types.
	//
	// Example:
	//
	// class Outer {
	// class Inner { }
	// }
	//
	// class ChildOfInner extends Outer.Inner {
	// ChildOfInner() { (new Outer()).super(); }
	// }
	if (n.qualifier() != null) {
	    if (n.kind() != ConstructorCall.SUPER) {
		throw new SemanticException("Can only qualify a \"super\"constructor invocation.", n.position());
	    }

	    if (!superType.isClass() || !superType.toClass().isInnerClass() || superType.toClass().inStaticContext()) {
		throw new SemanticException("The class \"" + superType + "\" is not an inner class, or was declared in a static "
		                            + "context; a qualified constructor invocation cannot be used.", n.position());
	    }

	    Type qt = n.qualifier().type();

	    if (!qt.isClass() || !qt.isSubtype(superType.toClass().outer(), c)) {
		throw new SemanticException("The type of the qualifier \"" + qt + "\" does not match the immediately enclosing "
		                            + "class  of the super class \"" + superType.toClass().outer() + "\".", n.qualifier().position());
	    }
	}

	if (n.kind() == ConstructorCall.SUPER) {
	    if (!superType.isClass()) {
		throw new SemanticException("Super type of " + ct + " is not a class.", n.position());
	    }

	    Expr q = n.qualifier();

	    // If the super class is an inner class (i.e., has an enclosing
	    // instance of its container class), then either a qualifier
	    // must be provided, or ct must have an enclosing instance of
	    // the superclass's container class, or a subclass thereof.
	    if (q == null && superType.isClass() && superType.toClass().isInnerClass()) {
		ClassType superContainer = superType.toClass().outer();
		// ct needs an enclosing instance of superContainer,
		// or a subclass of superContainer.
		ClassType e = ct;

		while (e != null) {
		    if (e.isSubtype(superContainer, c) && ct.hasEnclosingInstance(e)) {
			q = nf.This(n.position(), nf.CanonicalTypeNode(n.position(), e)).type(e);
			q = (Expr) setContext(q, c);
			q = (Expr) accept(q);
			break;
		    }
		    e = e.outer();
		}

		if (e == null) {
		    throw new SemanticException(ct + " must have an enclosing instance that is a subtype of " + superContainer, n.position());
		}
		if (e == ct) {
		    throw new SemanticException(ct + " is a subtype of " + superContainer + "; an enclosing instance that is a subtype of "
		                                + superContainer + " must be specified in the super constructor call.", n.position());
		}
	    }

	    if (n.qualifier() != q)
		n1 = (ConstructorCall_c) n1.qualifier(q);
	}

	List<Type> argTypes = new ArrayList<Type>();

	for (Expr e : n1.arguments()) {
	    argTypes.add(e.type());
	    
	    if (e.type() instanceof ErrorType) {
		return n1.constructorInstance(errorCI(n)).type(ts.Void());
	    }
	}

	if (n.kind() == ConstructorCall.SUPER) {
	    ct = ct.superClass().toClass();
	}

	ConstructorInstance ci = ts.findConstructor(ct, ts.ConstructorMatcher(ct, argTypes, c));

	n1 = (ConstructorCall_c) n1.constructorInstance(ci);

	n1 = (ConstructorCall_c) n1.arguments(convert(n1.arguments(), ci.formalTypes(), context));
	
	return n1.type(ts.Void());
    }

    public Node visit(New_c n, Context context) throws SemanticException {
	New_c n1 = typeCheckHeader(n, context);

	ClassBody body = (ClassBody) accept(n1.body());
	n1 = (New_c) n1.body(body);

	n1 = (New_c) tcNew(n, n1, context);
	
	ConstructorInstance ci = n1.constructorInstance();
	return n1.arguments(convert(n1.arguments(), ci.formalTypes(), context));
    }

    public Node tcNew(New_c old, New_c n, Context context) throws SemanticException {
	typeCheckFlags(n);
	typeCheckNested(n);

	ClassType ct = n.objectType().type().toClass();
	ConstructorInstance ci;

	if (!ct.flags().isInterface()) {
	    List<Type> argTypes = new ArrayList<Type>(n.arguments().size());
	    
	    for (Expr e : n.arguments()) {
		argTypes.add(e.type());
		if (e.type() instanceof ErrorType) {
		    return n.constructorInstance(errorCI(n)).type(ct);
		}
	    }
	    Context c = context;
	    if (n.anonType() != null) {
		c = c.pushClass(n.anonType(), n.anonType().asType());
	    }
	    ci = ts.findConstructor(ct, ts.ConstructorMatcher(ct, argTypes, c));
	}
	else {
	    ConstructorDef dci = ts.defaultConstructor(n.position(), Types.<ClassType> ref(ct));
	    ci = dci.asInstance();
	}

	New n1 = n.constructorInstance(ci);

	if (n.anonType() != null) {
	    // The type of the new expression is the anonymous type, not the
	    // base type.
	    ct = n.anonType().asType();
	}

	return n1.type(ct);
    }

    private void typeCheckFlags(New_c n) throws SemanticException {
	ClassType ct = n.objectType().type().toClass();

	if (n.body() == null) {
	    if (ct.flags().isInterface()) {
		throw new SemanticException("Cannot instantiate an interface.", n.position());
	    }

	    if (ct.flags().isAbstract()) {
		throw new SemanticException("Cannot instantiate an abstract class.", n.position());
	    }
	}
	else {
	    if (ct.flags().isFinal()) {
		throw new SemanticException("Cannot create an anonymous subclass of a final class.", n.position());
	    }

	    if (ct.flags().isInterface() && !n.arguments().isEmpty()) {
		throw new SemanticException("Cannot pass arguments to an anonymous class that implements an interface.", n.arguments().get(0).position());
	    }
	}
    }

    private void typeCheckNested(New_c n) throws SemanticException {
	if (n.qualifier() != null) {
	    // We have not disambiguated the type node yet.

	    // Get the qualifier type first.
	    Type qt = n.qualifier().type();

	    if (!qt.isClass()) {
		throw new SemanticException("Cannot instantiate member class of a non-class type.", n.qualifier().position());
	    }

	    // Disambiguate the type node as a member of the qualifier type.
	    ClassType ct = n.objectType().type().toClass();

	    // According to JLS2 15.9.1, the class type being
	    // instantiated must be inner.
	    if (! ct.isInnerClass()) {
		throw new SemanticException("Cannot provide a containing instance for non-inner class " + ct.fullName() + ".", n.qualifier().position());
	    }
	}
	else {
	    ClassType ct = n.objectType().type().toClass();

	    if (ct.isMember()) {
		for (ClassType t = ct; t.isMember(); t = t.outer()) {
		    if (! t.flags().isStatic()) {
			throw new SemanticException("Cannot allocate non-static member class \"" + t + "\".", n.position());
		    }
		}
	    }
	}
    }

    private New_c typeCheckHeader(New_c n, Context context) throws SemanticException {
	n = typeCheckObjectType(context, n);

	Expr qualifier = n.qualifier();
	TypeNode tn = n.objectType();
	List<Expr> arguments = n.arguments();
	ClassBody body = n.body();

//	if (body != null) {
//	    Ref<? extends Type> ct = tn.typeRef();
//	    ClassDef anonType = n.anonType();
//
//	    assert anonType != null;
//
//	    if (!ct.get().toClass().flags().isInterface()) {
//		anonType.superType(ct);
//	    }
//	    else {
//		anonType.superType(Types.<Type> ref(ts.Object()));
//		assert anonType.interfaces().isEmpty() || anonType.interfaces().get(0) == ct;
//		if (anonType.interfaces().isEmpty())
//		    anonType.addInterface(ct);
//	    }
//	}

	arguments = (List<Expr>) accept(arguments);

	n = (New_c) n.qualifier(qualifier);
	n = (New_c) n.objectType(tn);
	n = (New_c) n.arguments(arguments);
	n = (New_c) n.body(body);

	return n;
    }

    private New_c typeCheckObjectType(Context c, New_c n) throws SemanticException {
	Expr qualifier = n.qualifier();
	TypeNode tn = n.objectType();
	List<Expr> arguments = n.arguments();
	ClassBody body = n.body();

	if (qualifier == null) {
	    tn = (TypeNode) accept(tn);
	    // if (childtc.hasErrors()) throw new SemanticException();

	    if (tn.type() instanceof UnknownType) {
		throw new SemanticException();
	    }

	    if (tn.type().isClass()) {
		ClassType ct = tn.type().toClass();

		if (ct.isMember() && !ct.flags().isStatic()) {
		    New k = findQualifier(c, n, ct);
		    qualifier = (Expr) accept(k.qualifier());
		}
	    }
	    else {
		throw new SemanticException("Cannot instantiate type " + tn.type() + ".");
	    }
	}
	else {
	    qualifier = (Expr) accept(n.qualifier());

	    if (tn instanceof AmbTypeNode && ((AmbTypeNode) tn).child() instanceof QualifiedName && ((QualifiedName) ((AmbTypeNode) tn).child()).prefix() == null) {
		// We have to disambiguate the type node as if it were a
		// member of the
		// static type, outer, of the qualifier. For Java this is
		// simple: type
		// nested type is just a name and we
		// use that name to lookup a member of the outer class. For
		// some
		// extensions (e.g., PolyJ), the type node may be more
		// complex than
		// just a name. We'll just punt here and let the extensions
		// handle
		// this complexity.

		Name name = ((QualifiedName) ((AmbTypeNode) tn).child()).name().id();
		assert name != null;

		if (!qualifier.type().isClass()) {
		    throw new SemanticException("Cannot instantiate member class of non-class type.", n.position());
		}
		Type ct = ts.findMemberType(qualifier.type(), name, c);
		((Ref<Type>) tn.typeRef()).update(ct);
		tn = nf.CanonicalTypeNode(n.objectType().position(), tn.typeRef());
		tn = (TypeNode) copyAttributesFrom(tn, n.objectType());
	    }
	    else {
		throw new SemanticException("Only simply-named member classes may be instantiated by a qualified new expression.", tn.position());
	    }
	}

	n = (New_c) n.qualifier(qualifier);
	n = (New_c) n.objectType(tn);
	n = (New_c) n.arguments(arguments);
	n = (New_c) n.body(body);

	return n;
    }

    private New findQualifier(Context c, New_c n, ClassType ct) throws SemanticException {
	// If we're instantiating a non-static member class, add a "this"
	// qualifier.

	// Search for the outer class of the member.  The outer class is
	// not just ct.outer(); it may be a subclass of ct.outer().
	Type outer = null;

	Name name = ct.name();
	ClassType t = c.currentClass();

	// We're in one scope too many.
	if (t == n.anonType()) {
	    t = t.outer();
	}

	// Search all enclosing classes for the type.
	while (t != null) {
	    try {
		Type mt = ts.findMemberType(t, name, c);

		if (mt instanceof ClassType) {
		    ClassType cmt = (ClassType) mt;
		    if (cmt.def() == ct.def()) {
			outer = t;
			break;
		    }
		}
	    }
	    catch (SemanticException e) {
	    }

	    t = t.outer();
	}

	if (outer == null) {
	    throw new SemanticException("Could not find non-static member class \"" +
	                                name + "\".", n.position());
	}

	// Create the qualifier.
	Expr q;

	if (outer.typeEquals(c.currentClass(), c)) {
	    q = nf.This(n.position().startOf());
	}
	else {
	    q = nf.This(n.position().startOf(),
	                nf.CanonicalTypeNode(n.position(), outer));
	}

	q = (Expr) setContext(q, c);
	q = (Expr) accept(q);

	New k = n.qualifier(q);
	return k;
    }

    public Node visit(Case_c n, Context context) throws SemanticException {
	n = (Case_c) acceptChildren(n);

	n = (Case_c) n.type(ts.Void());
	
	if (n.expr() == null) {
	    return n;
	}

	Type t = n.expr().type();

	if (t instanceof ErrorType) {
	    return n;
	}

	if (! t.isIntOrLess()) {
	    throw new SemanticException("Case label must be a 32-bit integer or narrower.", n.position());
	}

	if (n.expr().isConstant()) {
	    Object o = n.expr().constantValue();

	    if (o instanceof Number && !(o instanceof Long) && !(o instanceof Float) && !(o instanceof Double)) {

		return n.value(((Number) o).longValue());
	    }
	    else if (o instanceof Character) {
		return n.value(((Character) o).charValue());
	    }
	}

	throw new SemanticException("Case label must be an integral constant.", n.position());
    }

    public Node visit(Block_c n, Context context) throws SemanticException {
	n = (Block_c) acceptChildren(n);
	return n.type(ts.Void());
    }

    public Node visit(Conditional_c n, Context context) throws SemanticException {
	n = (Conditional_c) acceptChildren(n);

	Expr e1 = n.consequent();
	Expr e2 = n.alternative();
	Type t1 = e1.type();
	Type t2 = e2.type();

	if (t1 instanceof ErrorType || t2 instanceof ErrorType) {
	    return n.type(ts.errorType(n.position()));
	}

	if (!(n.cond().type() instanceof ErrorType)) {
	    if (!n.cond().type().isBoolean()) {
		throw new SemanticException("Condition of ternary expression must be of type boolean.", n.cond().position());
	    }
	}

	// From the JLS, section:
	// If the second and third operands have the same type (which may be
	// the null type), then that is the type of the conditional
	// expression.
	if (ts.typeEquals(t1, t2, context)) {
	    return n.type(t1);
	}

	// Otherwise, if the second and third operands have numeric type,
	// then
	// there are several cases:
	if (t1.isNumeric() && t2.isNumeric()) {
	    // - If one of the operands is of type byte and the other is of
	    // type short, then the type of the conditional expression is
	    // short.
	    if (t1.isByte() && t2.isShort()) {
		return n.consequent(convert(n.consequent(), ts.Short(), context)).type(ts.Short());
	    }
	    if (t1.isShort() && t2.isByte()) {
		return n.alternative(convert(n.alternative(), ts.Short(), context)).type(ts.Short());
	    }

	    // - If one of the operands is of type T where T is byte, short,
	    // or
	    // char, and the other operand is a constant expression of type
	    // int
	    // whose value is representable in type T, then the type of the
	    // conditional expression is T.

	    if (t1.isIntOrLess() && t2.isInt() && ts.numericConversionValid(t1, e2.constantValue(), context)) {
		return n.alternative(convert(e2, t1, context)).type(t1);
	    }

	    if (t2.isIntOrLess() && t1.isInt() && ts.numericConversionValid(t2, e1.constantValue(), context)) {
		return n.consequent(convert(e1, t2, context)).type(t2);
	    }

	    // - Otherwise, binary numeric promotion (Sec. 5.6.2) is applied
	    // to the
	    // operand types, and the type of the conditional expression is
	    // the
	    // promoted type of the second and third operands. Note that
	    // binary
	    // numeric promotion performs value set conversion (Sec. 5.1.8).
	
	    return n.consequent(convert(e1, ts.promote(t1, t2), context)).alternative(convert(e2, ts.promote(t1, t2), context)).type(ts.promote(t1, t2));
	}

	// If one of the second and third operands is of the null type and
	// the
	// type of the other is a reference type, then the type of the
	// conditional expression is that reference type.
	if (t1.isNull() && t2.isReference())
	    return n.type(t2);
	if (t2.isNull() && t1.isReference())
	    return n.type(t1);

	// If the second and third operands are of different reference
	// types,
	// then it must be possible to convert one of the types to the other
	// type (call this latter type T) by assignment conversion (Sec.
	// 5.2); the
	// type of the conditional expression is T. It is a compile-time
	// error
	// if neither type is assignment compatible with the other type.

	if (t1.isReference() && t2.isReference()) {
	    if (ts.isImplicitCastValid(t1, t2, context)) {
		return n.consequent(convert(e1, t2, context)).type(t2);
	    }
	    if (ts.isImplicitCastValid(t2, t1, context)) {
		return n.consequent(convert(e2, t1, context)).type(t1);
	    }
	}

	throw new SemanticException("Could not determine type of ternary conditional expression; cannot assign " + t1 + " to " + t2 + " or vice versa.",
	                           n.position());
    }

    public Node visit(QualifiedName_c n, Context context) throws SemanticException {
	n = (QualifiedName_c) acceptChildren(n);

	Position pos = n.position();
	Node n1 = disamb(n, context, n.prefix(), n.name());
	n1 = copyAttributesFrom(n1, n);

	if (n1 != null) {
	    if (n1 != n)
		n1 = n1.accept(this, context);
	    return n1;
	}
	throw new SemanticException("Could not find " + n, pos);
    }
    
    public Node visit(Id_c n, Context context) throws SemanticException {
	return acceptChildren(n);
    }
    
    public Node visit(AmbExpr_c n, Context context) throws SemanticException {
	n = (AmbExpr_c) acceptChildren(n);

	Node n1 = n.child();

	if (n1 instanceof Expr) {
	    return n1;
	}

	throw new SemanticException("Could not find variable \"" + n.child() + "\".", n.position());
    }

    public Node visit(Field_c n, Context context) throws SemanticException {
	n = (Field_c) acceptChildren(n);

	Context c = context;
	
	if (n.target().type() instanceof ErrorType) {
	    return n.fieldInstance(errorFI(n)).type(ts.errorType(n.position()));
	}

	FieldInstance fi = ts.findField(n.target().type(), ts.FieldMatcher(n.target().type(), n.name().id(), c));

	if (fi == null) {
	    throw new InternalCompilerError("Cannot access field on node of type " + n.target().getClass().getName() + ".");
	}

	Field_c f1 = (Field_c) n.fieldInstance(fi).type(fi.type());
	f1.checkConsistency(c);

	return f1;
    }

    public Node visit(AmbTypeNode_c n, Context context) throws SemanticException {
	n = (AmbTypeNode_c) acceptChildren(n);

	assert n.context() == context : "context mismatch for " + n;
	assert context != null : "null context for " + n;

	SemanticException ex;

	    Node n1 = n.child();

	    if (n1 instanceof TypeNode) {
		TypeNode tn = (TypeNode) n1;
		Ref<Type> sym = n.typeRef();
		sym.update(tn.typeRef().get());
		return n1;
	    }

	    ex = new SemanticException("Could not find type \"" + n + "\".", n.position());

	// Mark the type as an error, so we don't try looking it up again.
	Ref<Type> sym = n.typeRef();
	sym.update(this.ts.unknownType(n.position()));

	throw ex;
    }

    private Node disamb(QualifiedName n, Context context, Node prefix, Id name) throws SemanticException {
	Node result = null;

	if (prefix instanceof PackageNode) {
	    PackageNode pn = (PackageNode) prefix;
	    result = disambPackagePrefix(n.position(), name, n, context, pn);
	}
	else if (prefix instanceof TypeNode) {
	    TypeNode tn = (TypeNode) prefix;
	    result = disambTypeNodePrefix(n.position(), prefix, name, n, context, tn);
	}
	else if (prefix instanceof Expr) {
	    Expr e = (Expr) prefix;
	    result = disambExprPrefix(n.position(), name, n, context, e);
	}
	else if (prefix == null) {
	    result = disambNoPrefix(n.position(), prefix, name, n, context);
	}

	return result;
    }

    private Node disambNoPrefix(Position pos, Node prefix, Id name, QualifiedName amb, Context c) throws SemanticException {
	if (exprOk(amb)) {
	    // First try local variables and fields.
	    assert c != null : "null context";
	    assert name != null : "null name";
	    VarInstance vi = c.findVariableSilent(name.id());

	    if (vi != null) {
		Node n = disambVarInstance(vi, pos, name, c);
		if (n != null) return n;
	    }
	}

	// no variable found. try types.
	if (typeOk(amb)) {
	    try {
		Named n = c.find(ts.TypeMatcher(name.id()));
		if (n instanceof Type) {
		    Type type = (Type) n;
		    return makeTypeNode(type, pos, amb);
		}
	    } catch (NoClassException e) {
		if (! name.id().toString().equals(e.getClassName())) {
		    // hmm, something else must have gone wrong
		    // rethrow the exception
		    throw e;
		}

		// couldn't find a type named name. 
		// It must be a package--ignore the exception.
	    }
	}

	// Must be a package then...
	if (packageOk(amb)) {
	    try {
		Package p = ts.packageForName(QName.make(null, name.id()));
		PackageNode pn = nf.PackageNode(pos, Types.ref(p));
		pn = (PackageNode) setContext(pn, c);
		pn = (PackageNode) accept(pn);
		return pn;
	    }
	    catch (SemanticException e) {
	    }
	    Package p = ts.createPackage(QName.make(null, name.id()));
	    PackageNode pn = nf.PackageNode(pos, Types.ref(p));
	    pn = (PackageNode) setContext(pn, c);
	    pn = (PackageNode) accept(pn);
	    return pn;
	}

	return null;
    }

    private boolean packageOk(QualifiedName amb) {
	return ! (amb instanceof Receiver) &&
	(amb instanceof QualifierNode || amb instanceof Node);
    }

    private Node disambVarInstance(VarInstance vi, Position pos, Id name, Context c) throws SemanticException {
	Node n = null;
	if (vi instanceof FieldInstance) {
	    FieldInstance fi = (FieldInstance) vi;
	    Receiver r = makeMissingFieldTarget(c, pos, name, fi);
	    n = nf.Field(pos, r, name).fieldInstance(fi).targetImplicit(true);
	}
	else if (vi instanceof LocalInstance) {
	    LocalInstance li = (LocalInstance) vi;
	    n = nf.Local(pos, name).localInstance(li);
	}
	if (n != null) {
	    n = setContext(n, c);
	    n = accept(n);
	}
	return n;
    }

    private Receiver makeMissingFieldTarget(Context c, Position pos, Id name, FieldInstance fi) throws SemanticException {
	Receiver r;

	if (fi.flags().isStatic()) {
	    r = nf.CanonicalTypeNode(pos.startOf(), fi.container());
	} else {
	    // The field is non-static, so we must prepend with
	    // "this", but we need to determine if the "this"
	    // should be qualified.  Get the enclosing class which
	    // brought the field into scope.  This is different
	    // from fi.container().  fi.container() returns a super
	    // type of the class we want.
	    ClassType scope = c.findFieldScope(name.id());
	    assert scope != null;

	    if (! ts.typeEquals(scope, c.currentClass(), c)) {
		r = (Special) nf.This(pos.startOf(), nf.CanonicalTypeNode(pos.startOf(), scope));
	    }
	    else {
		r = (Special) nf.This(pos.startOf());
	    }
	}

	r = (Receiver) setContext(r, c);
	r = (Receiver) accept(r);

	return r;
    }

    private TypeNode makeTypeNode(Type type, final Position pos, QualifiedName amb) {
	if (amb instanceof TypeNode) {
	    TypeNode tn = (TypeNode) amb;
	    Ref<Type> sym = tn.typeRef();
	    sym.update(type);

	    final QName name = ((ClassType) type).fullName();

	    // Reset the resolver goal to one that can run when the ref is deserialized.
	    sym.setResolver(new Ref. Callable<Type>() {
		public Type call() {
		    try {
			return (Type) ts.systemResolver().find(name);
		    }
		    catch (SemanticException e) {
			return ts.errorType(pos);
		    }
		}
	    }, null);

	    TypeNode n = nf.CanonicalTypeNode(pos, sym);
	    n = (TypeNode) setContext(n, amb.context());
	    return n;
	}

	TypeNode n = nf.CanonicalTypeNode(pos, type);
	n = (TypeNode) setContext(n, amb.context());
	return n;
    }

    private boolean typeOk(QualifiedName amb) {
	return ! (amb instanceof Expr) &&
	(amb instanceof TypeNode || amb instanceof QualifierNode ||
		amb instanceof Receiver || amb instanceof QualifiedName);
    }

    private boolean exprOk(QualifiedName amb) {
	return ! (amb instanceof QualifierNode) &&
	! (amb instanceof TypeNode) &&
	(amb instanceof Expr || amb instanceof Receiver ||
		amb instanceof QualifiedName);
    }

    private Node disambExprPrefix(Position pos, Id name, QualifiedName amb, Context c, Expr e) throws SemanticException {
	// Must be a non-static field.
	if (exprOk(amb)) {
	    Node n = nf.Field(pos, e, name);
	    n = setContext(n, c);
	    n = accept(n);
	    return n;
	}
	return null;
    }

    private Node disambTypeNodePrefix(Position pos, Node prefix, Id name, QualifiedName amb, Context c, TypeNode tn) throws SemanticException {
	// Try static fields.
	Type t = tn.type();

	if (exprOk(amb)) {
	    try {
		FieldInstance fi = ts.findField(t, ts.FieldMatcher(t, name.id(), c));
		Node n = nf.Field(pos, tn, name).fieldInstance(fi);
		n = setContext(n, c);
		n = accept(n);
		return n;
	    } catch (NoMemberException e) {
		if (e.getKind() != NoMemberException.FIELD) {
		    // something went wrong...
		    throw e;
		}

		// ignore so we can check if we're a member class.
	    }
	}

	// Try member classes.
	if (t.isClass() && typeOk(amb)) {
	    Resolver tc = t.toClass().resolver();
	    Named n;
	    try {
		n = tc.find(ts.MemberTypeMatcher(t, name.id(), c));
	    }
	    catch (NoClassException e) {
		return null;
	    }
	    if (n instanceof Type) {
		Type type = (Type) n;
		return makeTypeNode(type, pos, amb);
	    }
	}

	return null;
    }

    private Node disambPackagePrefix(Position pos, Id name, QualifiedName amb, Context c, PackageNode pn) throws SemanticException {
	Resolver pc = ts.packageContextResolver(pn.package_().get());

	Named n;

	try {
	    n = pc.find(ts.TypeMatcher(name.id()));
	}
	catch (SemanticException e) {
	    n = null;
	}

	Qualifier q = null;

	if (n instanceof Qualifier) {
	    q = (Qualifier) n;
	}
	else if (n == null) {
	    Package p = ts.createPackage(pn.package_(), name.id());
	    q = p;
	}
	else {
	    return null;
	}

	if (q.isPackage() && packageOk(amb)) {
	    Node n1 = nf.PackageNode(pos, Types.ref(q.toPackage()));
	    n1 = setContext(n1, c);
	    n1 = accept(n1);
	    return n1;
	}
	else if (q.isType() && typeOk(amb)) {
	    return makeTypeNode(q.toType(), pos, amb);
	}

	return null;
    }

    public Node visit(AmbQualifierNode_c n, Context context) throws SemanticException {
	n = (AmbQualifierNode_c) acceptChildren(n);

	SemanticException ex;

	Node n1 = n.child();

	if (n1 instanceof QualifierNode) {
	    QualifierNode qn = (QualifierNode) n1;
	    Qualifier q = qn.qualifierRef().get();
	    Ref<Qualifier> sym = n.qualifierRef();
	    sym.update(q);
	    return n1;
	}

	ex = new SemanticException("Could not find type or package \"" + n + "\".", n.position());

	// Mark the type as an error, so we don't try looking it up again.
	Ref<Qualifier> sym = n.qualifierRef();
	sym.update(this.ts.errorQualifier(n.position()));

	throw ex;
    }

    public Node visit(AmbReceiver_c n, Context context) throws SemanticException {
	n = (AmbReceiver_c) acceptChildren(n, context);

	if (n.child() instanceof Receiver) {
	    return n.child();
	}

	throw new SemanticException("Could not find type or variable \"" + n + "\".", n.position());
    }

    public Node visit(ArrayTypeNode_c n, Context context) throws SemanticException {
	n = (ArrayTypeNode_c) acceptChildren(n);
	CanonicalTypeNode n1 = nf.CanonicalTypeNode(n.position(), n.typeRef());
	if (! n.type().isArray())
	    throw new SemanticException("Invalid array type " + n.type() + ".", n.position());
	n1 = (CanonicalTypeNode) copyAttributesFrom(n1, n);
	return accept(n1);
    }

    private <T extends Node> T copyAttributesFrom(T neu, Node old) {
//	if (neu == null)
//	    return null;
//	Context context = old.context();
//	neu = (T) setContext(neu, context);
	return neu;
    }

    private <T extends Node> T setContext(T n, Context context) {
	if (n == null)
	    return null;

	ContextVisitor v = new ContextVisitor(job, ts, nf) {
	    @Override
	    protected Node leaveCall(Node n) throws SemanticException {
		if (n.context() != null) {
		    return n;
		}
		return n.context(context().freeze());
	    }
	};
	v = v.context(context);
	n = (T) n.visit(v);
	return n;
    }

    public Node visit(CanonicalTypeNode_c n, Context context) throws SemanticException {
	n = (CanonicalTypeNode_c) acceptChildren(n);

	if (n.typeRef().get().isClass()) {
	    ClassType ct = n.typeRef().get().toClass();
	    if (ct.isTopLevel() || ct.isMember()) {
		if (!ts.classAccessible(ct.def(), context)) {
		    throw new SemanticException("Cannot access class \"" + ct + "\" from the body of \"" + context.currentClass() + "\".", n.position());
		}
	    }
	}

	return n;
    }

    public Node visit(Try_c n, Context context) throws SemanticException {
	n = (Try_c) acceptChildren(n);
	return n.type(ts.Void());
    }

    public Node visit(Branch_c n, Context context) throws SemanticException {
	switch (n.kind()) {
	case BREAK:
	    if (n.labelNode() != null) {
		if (! context.hasBreakLabel(n.labelNode().id()))
		    throw new SemanticException("Label " + n.labelNode().id() + " is not a loop or switch label.", n.labelNode().position());
	    }
	    else {
		if (! context.hasBreakLabel(null))
		    throw new SemanticException("Cannot have a break statement outside of a loop or switch.", n.position());
	    }
	    break;
	case CONTINUE:
	    if (n.labelNode() != null) {
		if (! context.hasContinueLabel(n.labelNode().id()))
		    throw new SemanticException("Label " + n.labelNode().id() + " is not a loop label.", n.labelNode().position());
	    }
	    else {
		if (! context.hasContinueLabel(null))
		    throw new SemanticException("Cannot have a continue statement outside of a loop.", n.position());
	    }
	    break;
	}

	n = (Branch_c) acceptChildren(n);
	return n.type(ts.Void());
    }

    public Node visit(Catch_c n, Context context) throws SemanticException {
	n = (Catch_c) acceptChildren(n);

	if (!(n.catchType() instanceof ErrorType)) {
	    if (!n.catchType().isThrowable()) {
	        throw new SemanticException("Can only throw subclasses of \"" + ts.Throwable() + "\".", n.formal().position());
	    }
	}

	return n.type(ts.Void());
    }

    public Node visit(FlagsNode_c n, Context context) throws SemanticException {
	return (FlagsNode_c) acceptChildren(n);
    }

    public Node visit(Import_c n, Context context) throws SemanticException {
	n = (Import_c) acceptChildren(n);

	// Make sure the imported name exists.
	if (n.kind() == Import.PACKAGE && ts.systemResolver().packageExists(n.name()))
	    return n;

	Named n1;
	try {
	    n1 = ts.systemResolver().find(n.name());
	}
	catch (SemanticException e) {
	    throw new SemanticException("Package or class " + n.name() + " not found.");
	}

	if (n1 instanceof Type) {
	    Type t = (Type) n1;
	    if (t.isClass()) {
		ClassType ct = t.toClass();
		if (!ts.classAccessibleFromPackage(ct.def(), context.package_())) {
		    throw new SemanticException("Class " + ct + " is not accessible.");
		}
	    }
	}

	return n;
    }

    public Node visit(Return_c n, Context context) throws SemanticException {
	n = (Return_c) acceptChildren(n);

	n = (Return_c) n.type(ts.Void());
	
	Context c = context;

	CodeDef ci = c.currentCode();

	if (ci instanceof InitializerDef) {
	    throw new SemanticException("Cannot return from an initializer block.", n.position());
	}

	if (ci instanceof ConstructorDef) {
	    if (n.expr() != null) {
		throw new SemanticException("Cannot return a value from " + ci + ".", n.position());
	    }

	    return n;
	}

	if (ci instanceof FunctionDef) {
	    FunctionDef fi = (FunctionDef) ci;
	    Type returnType = Types.get(fi.returnType());

	    
	    if (returnType == null) {
		throw new InternalCompilerError("Null return type for " + fi);
	    }

	    if (returnType instanceof ErrorType) {
		throw new SemanticException();
	    }

	    if (returnType instanceof UnknownType) {
		throw new SemanticException();
	    }

	    if (returnType.isVoid()) {
		if (n.expr() != null) {
		    throw new SemanticException("Cannot return a value from " + fi + ".", n.position());
		}
		else {
		    return n;
		}
	    }
	    else if (n.expr() == null) {
		throw new SemanticException("Must return a value from " + fi + ".", n.position());
	    }

	    if (n.expr().type() instanceof ErrorType) {
		throw new SemanticException();
	    }

	    if (ts.isImplicitCastValid(n.expr().type(), returnType, c)) {
		return n.expr(convert(n.expr(), returnType, c));
	    }

	    if (ts.numericConversionValid(returnType, n.expr().constantValue(), c)) {
		return n;
	    }

	    throw new SemanticException("Cannot return expression of type " + n.expr().type() + " from " + fi + ".", n.expr().position());
	}

	throw new SemanticException("Cannot return from this context.", n.position());
    }

    public Node visit(PackageNode_c n, Context context) throws SemanticException {
	return (PackageNode_c) acceptChildren(n);
    }

    public Node visit(Throw_c n, Context context) throws SemanticException {
	n = (Throw_c) acceptChildren(n);

	if (!(n.expr().type() instanceof ErrorType)) {
	    if (!n.expr().type().isThrowable()) {
	        throw new SemanticException("Can only throw subclasses of \"" + this.ts.Throwable() + "\".", n.expr().position());
	    }
	}

	return n.type(ts.Void());
    }

    public Node visit(MethodDecl_c n, Context context) throws SemanticException {
	n = (MethodDecl_c) acceptChildren(n);

	for (TypeNode tn : n.throwTypes()) {
	    Type t = tn.type();
	    if (!t.isThrowable()) {
		throw new SemanticException("Type \"" + t + "\" is not a subclass of \"" + ts.Throwable() + "\".", tn.position());
	    }
	}

	return n;
    }

    public Node visit(FieldDecl_c n, Context context) throws SemanticException {
	n = (FieldDecl_c) acceptChildren(n);

	if (n.init() != null && !(n.init().type() instanceof UnknownType)) {
	    if (n.init() instanceof ArrayInit) {
		ArrayInit init = (ArrayInit) n.init();
		init = typeCheckElements(context, n.type().type(), init);
		n = (FieldDecl_c) n.init(init);
	    }
	    else {
		if (!(n.init().type() instanceof ErrorType)) {
		    if (!ts.isImplicitCastValid(n.init().type(), n.type().type(), context) && !ts.typeEquals(n.init().type(), n.type().type(), context)
		    	&& !ts.numericConversionValid(n.type().type(), n.init().constantValue(), context)) {

		        throw new SemanticException("The type of the variable initializer \"" + n.init().type() + "\" does not match that of "
		                                    + "the declaration \"" + n.type().type() + "\".", n.init().position());
		    }
		}

		n = (FieldDecl_c) n.init(convert(n.init(), n.type().type(), context));
	    }
	}

	if (n.init() == null || !n.init().isConstant() || !n.fieldDef().flags().isFinal()) {
	    n.fieldDef().setNotConstant();
	}
	else {
	    n.fieldDef().setConstantValue(n.init().constantValue());
	}

	return n;
    }

    public Node visit(ConstructorDecl_c n, Context context) throws SemanticException {
	n = (ConstructorDecl_c) acceptChildren(n);

	for (TypeNode tn : n.throwTypes()) {
	    Type t = tn.type();
	    if (!t.isThrowable()) {
		throw new SemanticException("Type \"" + t + "\" is not a subclass of \"" + ts.Throwable() + "\".", tn.position());
	    }
	}

	return n;
    }

    public Node visit(ClassBody_c n, Context context) throws SemanticException {
	return (ClassBody_c) acceptChildren(n);
    }

    public Node visit(SwitchBlock_c n, Context context) throws SemanticException {
	n = (SwitchBlock_c) acceptChildren(n);
	return n.type(ts.Void());
    }

    public Node visit(LocalClassDecl_c n, Context context) throws SemanticException {
	return (LocalClassDecl_c) acceptChildren(n);
    }

    public Node visit(IntLit_c n, Context context) throws SemanticException {
	polyglot.ast.IntLit.Kind kind = n.kind();

	if (kind == IntLit.INT) {
	    return n.type(ts.Int());
	}
	else if (kind == IntLit.LONG) {
	    return n.type(ts.Long());
	}
	else {
	    throw new InternalCompilerError("Unrecognized IntLit kind " + kind);
	}
    }

    public Node visit(FloatLit_c n, Context context) throws SemanticException {
	polyglot.ast.FloatLit.Kind kind = n.kind();

	if (kind == FloatLit.FLOAT) {
	    return n.type(ts.Float());
	}
	else if (kind == FloatLit.DOUBLE) {
	    return n.type(ts.Double());
	}
	else {
	    throw new InternalCompilerError("Unrecognized FloatLit kind " + kind);
	}
    }

    public Node visit(StringLit_c n, Context context) throws SemanticException {
	return n.type(ts.String());
    }

    public Node visit(CharLit_c n, Context context) throws SemanticException {
	return n.type(ts.Char());
    }

    public Node visit(BooleanLit_c n, Context context) throws SemanticException {
	return n.type(ts.Boolean());
    }

    public Node visit(NullLit_c n, Context context) throws SemanticException {
	return n.type(ts.Null());
    }

    public Node visit(ClassLit_c n, Context context) throws SemanticException {
	n = (ClassLit_c) acceptChildren(n);
	return n.type(ts.Class());
    }
    public Node visit(Instanceof_c n, Context context) throws SemanticException {
	n = (Instanceof_c) acceptChildren(n);

	if (n.expr().type() instanceof ErrorType || n.compareType().type() instanceof ErrorType) {
	    return n.type(ts.Boolean());
	}
	
	if (! n.compareType().type().isReference()) {
	    throw new SemanticException(
	                                "Type operand " + n.compareType().type() + " must be a reference type.",
	                                n.compareType().position());
	}

	if (! ts.isCastValid(n.expr().type(), n.compareType().type(), context)) {
	    throw new SemanticException(
	                                "Expression operand type " + n.expr().type() + " incompatible with type operand " + n.compareType().type() + ".",
	                                n.expr().position());
	}

	return n.type(ts.Boolean());
    }

    public Node visit(ClassDecl_c n, Context context) throws SemanticException {
	n = (ClassDecl_c) acceptChildren(n, context);
	
	checkSuperTypes(n);
	
	return n;
    }

    private void checkSuperTypes(ClassDecl_c n) throws SemanticException {
	Ref<? extends Type> stref = n.classDef().superType();
	if (stref != null) {
	    Type t = stref.get();
	    if (t instanceof ErrorType)
		throw new SemanticException(); // already reported
	    if (!t.isClass() || t.toClass().flags().isInterface()) {
		throw new SemanticException("Cannot extend type " + t + "; not a class.", n.superClass() != null ? n.superClass().position() : n.position());
	    }
	}

	for (Ref<? extends Type> tref : n.classDef().interfaces()) {
	    Type t = tref.get();
	    if (t instanceof ErrorType)
		throw new SemanticException(); // already reported
	    if (!t.isClass() || !t.toClass().flags().isInterface()) {
		String s = n.classDef().flags().isInterface() ? "extend" : "implement";
		throw new SemanticException("Cannot " + s + " type " + t + "; not an interface.", n.position());
	    }
	}
    }

    public Node visit(Initializer_c n, Context context) throws SemanticException {
	n = (Initializer_c) acceptChildren(n);

	Flags flags = n.flags().flags();

	try {
	    ts.checkInitializerFlags(flags);
	}
	catch (SemanticException e) {
	    throw new SemanticException(e.getMessage(), n.position());
	}

	// check that inner classes do not declare static initializers
	if (flags.isStatic() && n.initializerDef().container().get().toClass().isInnerClass()) {
	    // it's a static initializer in an inner class.
	    throw new SemanticException("Inner classes cannot declare static initializers.", n.position());
	}

	return n;
    }

    public Node visit(Formal_c n, Context context) throws SemanticException {
	n = (Formal_c) acceptChildren(n);
	
	// Check if the variable is multiply defined.
	Context c = context;

	LocalInstance outerLocal = null;

	try {
	    outerLocal = c.findLocal(n.localDef().name());
	}
	catch (SemanticException e) {
	    // not found, so not multiply defined
	}

	if (outerLocal != null && !n.localDef().equals(outerLocal.def()) && c.isLocal(n.localDef().name())) {
	    throw new SemanticException("Local variable \"" + n.name() + "\" multiply defined.  Previous definition at " + outerLocal.position() + ".",
					n.position());
	}

	try {
	    ts.checkLocalFlags(n.flags().flags());
	}
	catch (SemanticException e) {
	    throw new SemanticException(e.getMessage(), n.position());
	}

	return n;
    }

    public Node visit(SourceFile_c n, Context context) throws SemanticException {
	n = (SourceFile_c) acceptChildren(n);

	Set<Name> names = new HashSet<Name>();
	boolean hasPublic = false;

	for (TopLevelDecl d : n.decls()) {
	    if (d.name() == null)
		continue;

	    Name s = d.name().id();

	    if (names.contains(s)) {
		throw new SemanticException("Duplicate declaration: \"" + s + "\".", d.position());
	    }

	    names.add(s);

	    if (d.flags().flags().isPublic()) {
		if (hasPublic) {
		    throw new SemanticException("The source contains more than one public declaration.", d.position());
		}

		hasPublic = true;
	    }
	}

	return n;
    }
}
