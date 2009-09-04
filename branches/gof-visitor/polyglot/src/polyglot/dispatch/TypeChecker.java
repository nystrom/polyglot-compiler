package polyglot.dispatch;

import java.util.*;

import polyglot.ast.*;
import polyglot.frontend.*;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.*;
import polyglot.visit.*;

public class TypeChecker {

    Job job;
    TypeSystem ts;
    NodeFactory nf;

    public TypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
    }

    public Node visit(Node n) {
	if (n == null)
	    return n;
	return n.checked();
    }

    Node accept(Node n) {
	if (n == null)
	    return null;
	Node m = n.checked();
//	return n.accept(DispatchedTypeChecker.this);
	
	assert m != null : "null checked for " + n;
	
	return m;
    }

    List<? extends Node> accept(List<? extends Node> l) {
	List result = new ArrayList();
	for (Node n : l) {
	    Node n2 = accept(n);
	    result.add(n2);
	}
	return result;
    }

    public Node acceptChildren(Node n) {
	return n.visitChildren(new NodeVisitor() {
	    public Node override(Node n) {
		return TypeChecker.this.visit(n);
	    }
	});
    }

    public Node visit(Node_c n, Context context) throws SemanticException {
	System.out.println("missing node " + n + " instanceof " + n.getClass().getName());
	return (Node_c) acceptChildren(n);
    }

    public Node visit(NewArray_c n, Context context) throws SemanticException {
	for (Expr expr : n.dims()) {
	    if (! ts.isImplicitCastValid(expr.type(), ts.Int(), context)) {
		throw new SemanticException("Array dimension must be an integer.",
		                            expr.position());
	    }
	}

	Type type = ts.arrayOf(n.baseType().type(), n.dims().size() + n.additionalDims());

	ArrayInit init = n.init();
	if (init != null) {
	    typeCheckElements(context, type, init);
	}

	return n.type(type);
    }

    private void typeCheckElements(Context context, Type type, ArrayInit init) throws SemanticException {
	if (! type.isArray()) {
	    throw new SemanticException("Cannot initialize " + type +
	                                " with " + init.type() + ".", init.position());
	}

	// Check if we can assign each individual element.
	Type t = type.toArray().base();

	for (Expr e : init.elements()) {
	    Type s = e.type();

	    if (e instanceof ArrayInit) {
		typeCheckElements(context, t, (ArrayInit) e);
		continue;
	    }

	    if (! ts.isImplicitCastValid(s, t, context) &&
		    ! ts.typeEquals(s, t, context) &&
		    ! ts.numericConversionValid(t, e.constantValue(), context)) {
		throw new SemanticException("Cannot assign " + s +
		                            " to " + t + ".", e.position());
	    }
	}
    }

    public Node visit(LocalAssign_c n, Context context) throws SemanticException {
	return visit((Assign_c) n, context);
    }

    public Node visit(FieldAssign_c n, Context context) throws SemanticException {
	return visit((Assign_c) n, context);
    }

    public Node visit(ArrayAccessAssign_c n, Context context) throws SemanticException {
	return visit((Assign_c) n, context);
    }

    public Node visit(Assign_c n, Context context) throws SemanticException {
	n = (Assign_c) acceptChildren(n);

	Assign_c n1 = typeCheckLeft(n, context);

	Type t = n1.leftType();

	if (t == null)
	    t = ts.unknownType(n.position());

	Expr right = n1.right();
	Assign.Operator op = n1.operator();

	Type s = right.type();

	if (op == Assign_c.ASSIGN) {
	    if (!ts.isImplicitCastValid(s, t, context) && !ts.typeEquals(s, t, context) && !ts.numericConversionValid(t, right.constantValue(), context)) {

		throw new SemanticException("Cannot assign " + s + " to " + t + ".", n.position());
	    }

	    return n1.type(t);
	}

	if (op == Assign_c.ADD_ASSIGN) {
	    // t += s
	    if (ts.typeEquals(t, ts.String(), context) && ts.canCoerceToString(s, context)) {
		return n1.type(ts.String());
	    }

	    if (t.isNumeric() && s.isNumeric()) {
		return n1.type(ts.promote(t, s));
	    }

	    throw new SemanticException("The " + op + " operator must have " + "numeric or String operands.", n.position());
	}

	if (op == Assign_c.SUB_ASSIGN || op == Assign_c.MUL_ASSIGN || op == Assign_c.DIV_ASSIGN || op == Assign_c.MOD_ASSIGN) {
	    if (t.isNumeric() && s.isNumeric()) {
		return n1.type(ts.promote(t, s));
	    }

	    throw new SemanticException("The " + op + " operator must have " + "numeric operands.", n.position());
	}

	if (op == Assign_c.BIT_AND_ASSIGN || op == Assign_c.BIT_OR_ASSIGN || op == Assign_c.BIT_XOR_ASSIGN) {
	    if (t.isBoolean() && s.isBoolean()) {
		return n1.type(ts.Boolean());
	    }

	    if (ts.isImplicitCastValid(t, ts.Long(), context) && ts.isImplicitCastValid(s, ts.Long(), context)) {
		return n1.type(ts.promote(t, s));
	    }

	    throw new SemanticException("The " + op + " operator must have " + "integral or boolean operands.", n.position());
	}

	if (op == Assign_c.SHL_ASSIGN || op == Assign_c.SHR_ASSIGN || op == Assign_c.USHR_ASSIGN) {
	    if (ts.isImplicitCastValid(t, ts.Long(), context) && ts.isImplicitCastValid(s, ts.Long(), context)) {
		// Only promote the left of a shift.
		return n1.type(ts.promote(t));
	    }

	    throw new SemanticException("The " + op + " operator must have " + "integral operands.", n.position());
	}

	throw new InternalCompilerError("Unrecognized assignment operator " + op + ".");
    }

    private Assign_c typeCheckLeft(Assign_c n, Context context) throws SemanticException {
	if (n instanceof LocalAssign_c) {
	    LocalAssign_c a = (LocalAssign_c) n;
	    return (Assign_c) a;
	}
	else if (n instanceof FieldAssign_c) {
	    FieldAssign_c a = (FieldAssign_c) n;
	    Field left = (Field) a.left(nf);
	    left = (Field) visit((Field_c) left, context);
	    FieldAssign_c n1 = (FieldAssign_c) a.target(left.target());
	    n1 = (FieldAssign_c) n1.name(left.name());
	    return (Assign_c) n1.fieldInstance(left.fieldInstance());
	}
	else if (n instanceof ArrayAccessAssign_c) {
	    ArrayAccessAssign_c a = (ArrayAccessAssign_c) n;
	    Type at = a.array().type();
	    if (!at.isArray())
		throw new SemanticException("Target of array assignment is not an array element.", a.array().position());
	    Type it = a.index().type();
	    if (!it.isInt())
		throw new SemanticException("Array element must be indexed by an int.", a.index().position());
	    return a;
	}
	return n;
    }

    public Node visit(ArrayAccess_c n, Context context) throws SemanticException {
	n = (ArrayAccess_c) acceptChildren(n);

	if (!n.array().type().isArray()) {
	    throw new SemanticException("Subscript can only follow an array type.", n.position());
	}

	if (!ts.isImplicitCastValid(n.index().type(), ts.Int(), context)) {
	    throw new SemanticException("Array subscript must be an integer.", n.position());
	}

	return n.type(n.array().type().toArray().base());
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

	if (!n.cond().type().isBoolean()) {
	    throw new SemanticException("Condition of assert statement " + "must have boolean type.", n.cond().position());
	}

	if (n.errorMessage() != null && ts.typeEquals(n.errorMessage().type(), ts.Void(), context)) {
	    throw new SemanticException("Error message in assert statement " + "must have a value.", n.errorMessage().position());
	}

	return n;
    }

    public Node visit(Binary_c n, Context context) throws SemanticException {
	n = (Binary_c) acceptChildren(n);

	Type l = n.left().type();
	Type r = n.right().type();

	if (n.operator() == Binary_c.GT || n.operator() == Binary_c.LT || n.operator() == Binary_c.GE || n.operator() == Binary_c.LE) {
	    if (!l.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + l + ".", n.left().position());
	    }

	    if (!r.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + r + ".", n.right().position());
	    }

	    return n.type(ts.Boolean());
	}

	if (n.operator() == Binary_c.EQ || n.operator() == Binary_c.NE) {
	    if (!ts.isCastValid(l, r, context) && !ts.isCastValid(r, l, context)) {
		throw new SemanticException("The " + n.operator() + " operator must have operands of similar type.", n.position());
	    }

	    return n.type(ts.Boolean());
	}

	if (n.operator() == Binary_c.COND_OR || n.operator() == Binary_c.COND_AND) {
	    if (!l.isBoolean()) {
		throw new SemanticException("The " + n.operator() + " operator must have boolean operands, not type " + l + ".", n.left().position());
	    }

	    if (!r.isBoolean()) {
		throw new SemanticException("The " + n.operator() + " operator must have boolean operands, not type " + r + ".", n.right().position());
	    }

	    return n.type(ts.Boolean());
	}

	if (n.operator() == Binary_c.ADD) {
	    if (ts.isSubtype(l, ts.String(), context) || ts.isSubtype(r, ts.String(), context)) {
		if (!ts.canCoerceToString(r, context)) {
		    throw new SemanticException("Cannot coerce an expression " + "of type " + r + " to a String.", n.right().position());
		}
		if (!ts.canCoerceToString(l, context)) {
		    throw new SemanticException("Cannot coerce an expression " + "of type " + l + " to a String.", n.left().position());
		}

		return n.precedence(Precedence.STRING_ADD).type(ts.String());
	    }
	}

	if (n.operator() == Binary_c.BIT_AND || n.operator() == Binary_c.BIT_OR || n.operator() == Binary_c.BIT_XOR) {
	    if (l.isBoolean() && r.isBoolean()) {
		return n.type(ts.Boolean());
	    }
	}

	if (n.operator() == Binary_c.ADD) {
	    if (!l.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric or String operands, not type " + l + ".", n.left()
		                            .position());
	    }

	    if (!r.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric or String operands, not type " + r + ".", n.right()
		                            .position());
	    }
	}

	if (n.operator() == Binary_c.BIT_AND || n.operator() == Binary_c.BIT_OR || n.operator() == Binary_c.BIT_XOR) {
	    if (!ts.isImplicitCastValid(l, ts.Long(), context)) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric or boolean operands, not type " + l + ".", n.left()
		                            .position());
	    }

	    if (!ts.isImplicitCastValid(r, ts.Long(), context)) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric or boolean operands, not type " + r + ".", n.right()
		                            .position());
	    }
	}

	if (n.operator() == Binary_c.SUB || n.operator() == Binary_c.MUL || n.operator() == Binary_c.DIV || n.operator() == Binary_c.MOD) {
	    if (!l.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + l + ".", n.left().position());
	    }

	    if (!r.isNumeric()) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + r + ".", n.right().position());
	    }
	}

	if (n.operator() == Binary_c.SHL || n.operator() == Binary_c.SHR || n.operator() == Binary_c.USHR) {
	    if (!ts.isImplicitCastValid(l, ts.Long(), context)) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + l + ".", n.left().position());
	    }

	    if (!ts.isImplicitCastValid(r, ts.Long(), context)) {
		throw new SemanticException("The " + n.operator() + " operator must have numeric operands, not type " + r + ".", n.right().position());
	    }
	}

	if (n.operator() == Binary_c.SHL || n.operator() == Binary_c.SHR || n.operator() == Binary_c.USHR) {
	    // For shift, only promote the left operand.
	    return n.type(ts.promote(l));
	}

	return n.type(ts.promote(l, r));
    }

    public Node visit(Unary_c n, Context context) throws SemanticException {
	n = (Unary_c) acceptChildren(n);

	if (n.operator() == Unary_c.POST_INC || n.operator() == Unary_c.POST_DEC || n.operator() == Unary_c.PRE_INC || n.operator() == Unary_c.PRE_DEC) {

	    if (!n.expr().type().isNumeric()) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be numeric.", n.expr().position());
	    }

	    if (!(n.expr() instanceof Variable)) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be a variable.", n.expr().position());
	    }

	    if (((Variable) n.expr()).flags().isFinal()) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be a non-final variable.", n.expr().position());
	    }

	    return n.type(n.expr().type());
	}

	if (n.operator() == Unary_c.BIT_NOT) {
	    if (!ts.isImplicitCastValid(n.expr().type(), ts.Long(), context)) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be numeric.", n.expr().position());
	    }

	    return n.type(ts.promote(n.expr().type()));
	}

	if (n.operator() == Unary_c.NEG || n.operator() == Unary_c.POS) {
	    if (!n.expr().type().isNumeric()) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be numeric.", n.expr().position());
	    }

	    return n.type(ts.promote(n.expr().type()));
	}

	if (n.operator() == Unary_c.NOT) {
	    if (!n.expr().type().isBoolean()) {
		throw new SemanticException("Operand of " + n.operator() + " operator must be boolean.", n.expr().position());
	    }

	    return n.type(n.expr().type());
	}

	return n;
    }

    public Node visit(While_c n, Context context) throws SemanticException {
	n = (While_c) acceptChildren(n);

	if (!ts.typeEquals(n.cond().type(), ts.Boolean(), context)) {
	    throw new SemanticException("Condition of while statement must have boolean type.", n.cond().position());
	}

	return n;
    }

    public Node visit(Do_c n, Context context) throws SemanticException {
	n = (Do_c) acceptChildren(n);

	if (!ts.typeEquals(n.cond().type(), ts.Boolean(), context)) {
	    throw new SemanticException("Condition of do statement must have boolean type.", n.cond().position());
	}

	return n;
    }

    public Node visit(If_c n, Context context) throws SemanticException {
	n = (If_c) acceptChildren(n);

	if (!ts.typeEquals(n.cond().type(), ts.Boolean(), context)) {
	    throw new SemanticException("Condition of if statement must have boolean type.", n.cond().position());
	}

	return n;
    }

    public Node visit(Empty_c n, Context context) throws SemanticException {
	return (Empty_c) acceptChildren(n);
    }
    public Node visit(Eval_c n, Context context) throws SemanticException {
	return (Eval_c) acceptChildren(n);
    }
    public Node visit(Labeled_c n, Context context) throws SemanticException {
	return (Labeled_c) acceptChildren(n);
    }

    public Node visit(Switch_c n, Context context) throws SemanticException {
	n = (Switch_c) acceptChildren(n);

	if (!ts.isImplicitCastValid(n.expr().type(), ts.Int(), context) && !ts.isImplicitCastValid(n.expr().type(), ts.Char(), context)) {
	    throw new SemanticException("Switch index must be an integer.", n.position());
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

	return n;
    }

    public Node visit(Call_c n, Context context) throws SemanticException {
	n = (Call_c) acceptChildren(n);

	Context c = context;

	List<Type> argTypes = new ArrayList<Type>(n.arguments().size());

	for (Expr e : n.arguments()) {
	    argTypes.add(e.type());
	}

	if (n.target() == null) {
	    return typeCheckNullTarget(n, context, argTypes);
	}

	Type targetType = n.target().type();
	MethodInstance mi = ts.findMethod(targetType, ts.MethodMatcher(targetType, n.name().id(), argTypes, c));

	/*
	 * This call is in a static context if and only if the target
	 * (possibly implicit) is a type node.
	 */
	boolean staticContext = (n.target() instanceof TypeNode);

	if (staticContext && !mi.flags().isStatic()) {
	    throw new SemanticException("Cannot call non-static method " + n.name().id() + " of " + n.target().type() + " in static " + "context.",
	                                n.position());
	}

	// If the target is super, but the method is abstract, then
	// complain.
	if (n.target() instanceof Special && ((Special) n.target()).kind() == Special.SUPER && mi.flags().isAbstract()) {
	    throw new SemanticException("Cannot call an abstract method " + "of the super class", n.position());
	}

	Call_c call = (Call_c) n.methodInstance(mi).type(mi.returnType());

	// If we found a method, the call must type check, so no need to
	// check
	// the arguments here.
	call.checkConsistency(c);

	return call;
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
		Type dt = d.type().type();
		if (t == null) {
		    t = dt;
		}
		else if (!t.typeEquals(dt, context)) {
		    throw new InternalCompilerError("Local variable " + "declarations in a for loop initializer must all "
		                                    + "be the same type, in this case " + t + ", not " + dt + ".", d.position());
		}
	    }
	}

	if (n.cond() != null && !ts.isImplicitCastValid(n.cond().type(), ts.Boolean(), context)) {
	    throw new SemanticException("The condition of a for statement must have boolean type.", n.cond().position());
	}

	return n;
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

	if (outerLocal != null && c.isLocal(n.localDef().name())) {
	    throw new SemanticException(
	                                "Local variable \"" + n.name() + "\" multiply defined.  " + "Previous definition at " + outerLocal.position() + ".",
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
		typeCheckElements(context, n.type().type(), (ArrayInit) n.init());
	    }
	    else {
		if (!ts.isImplicitCastValid(n.init().type(), n.type().type(), context) && !ts.typeEquals(n.init().type(), n.type().type(), context)
			&& !ts.numericConversionValid(n.type().type(), n.init().constantValue(), context)) {
		    throw new SemanticException("The type of the variable " + "initializer \"" + n.init().type() + "\" does not match that of "
		                                + "the declaration \"" + n.type().type() + "\".", n.init().position());
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
		throw new SemanticException("Local variable \"" + li.name() + "\" is accessed from an inner class, and must be declared " + "final.",
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
		    throw new SemanticException("The nested class \"" + c.currentClass() + "\" does not have " + "an enclosing instance of type \"" + t
		                                + "\".", n.qualifier().position());
		}
	    }
	    else {
		throw new SemanticException("Invalid qualifier for \"this\" or \"super\".", n.qualifier().position());
	    }
	}

	if (t == null || (c.inStaticContext() && ts.typeEquals(t, c.currentClass(), c))) {
	    // trying to access "this" or "super" from a static context.
	    throw new SemanticException("Cannot access a non-static " + "member or refer to \"this\" or \"super\" " + "from a static context.",
	                                n.position());
	}

	if (n.kind() == Special_c.THIS) {
	    return n.type(t);
	}
	else if (n.kind() == Special_c.SUPER) {
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
	    if (n.kind() != ConstructorCall_c.SUPER) {
		throw new SemanticException("Can only qualify a \"super\"" + "constructor invocation.", n.position());
	    }

	    if (!superType.isClass() || !superType.toClass().isInnerClass() || superType.toClass().inStaticContext()) {
		throw new SemanticException("The class \"" + superType + "\"" + " is not an inner class, or was declared in a static "
		                            + "context; a qualified constructor invocation cannot " + "be used.", n.position());
	    }

	    Type qt = n.qualifier().type();

	    if (!qt.isClass() || !qt.isSubtype(superType.toClass().outer(), c)) {
		throw new SemanticException("The type of the qualifier " + "\"" + qt + "\" does not match the immediately enclosing "
		                            + "class  of the super class \"" + superType.toClass().outer() + "\".", n.qualifier().position());
	    }
	}

	if (n.kind() == ConstructorCall_c.SUPER) {
	    if (!superType.isClass()) {
		throw new SemanticException("Super type of " + ct + " is not a class.", n.position());
	    }

	    Expr q = n.qualifier();

	    // If the super class is an inner class (i.e., has an enclosing
	    // instance of its container class), then either a qualifier
	    // must be provided, or ct must have an enclosing instance of
	    // the
	    // super class's container class, or a subclass thereof.
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
		    throw new SemanticException(ct + " must have an enclosing instance" + " that is a subtype of " + superContainer, n.position());
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
	}

	if (n.kind() == ConstructorCall_c.SUPER) {
	    ct = ct.superClass().toClass();
	}

	ConstructorInstance ci = ts.findConstructor(ct, ts.ConstructorMatcher(ct, argTypes, c));

	return n1.constructorInstance(ci);
    }

    public Node visit(New_c n, Context context) throws SemanticException {
	New_c n1 = typeCheckerHeader(n, context);

	ClassBody body = (ClassBody) accept(n1.body());
	n1 = (New_c) n1.body(body);

	return tcNew(n, n1, context);
    }

    public Node tcNew(New_c old, New_c n, Context context) throws SemanticException {

	List<Type> argTypes = new ArrayList<Type>(n.arguments().size());

	for (Expr e : n.arguments()) {
	    argTypes.add(e.type());
	}

	typeCheckFlags(n);
	typeCheckNested(n);

	ClassType ct = n.objectType().type().toClass();
	ConstructorInstance ci;

	if (!ct.flags().isInterface()) {
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
		throw new SemanticException(
		                            "Cannot instantiate an interface.", n.position());
	    }

	    if (ct.flags().isAbstract()) {
		throw new SemanticException(
		                            "Cannot instantiate an abstract class.", n.position());
	    }
	}
	else {
	    if (ct.flags().isFinal()) {
		throw new SemanticException(
		                            "Cannot create an anonymous subclass of a final class.",
		                            n.position());
	    }

	    if (ct.flags().isInterface() && ! n.arguments().isEmpty()) {
		throw new SemanticException(
		                            "Cannot pass arguments to an anonymous class that " +
		                            "implements an interface.",
		                            n.arguments().get(0).position());
	    }
	}
    }

    private void typeCheckNested(New_c n) throws SemanticException {
	if (n.qualifier() != null) {
	    // We have not disambiguated the type node yet.

	    // Get the qualifier type first.
	    Type qt = n.qualifier().type();

	    if (! qt.isClass()) {
		throw new SemanticException(
		                            "Cannot instantiate member class of a non-class type.",
		                            n.qualifier().position());
	    }

	    // Disambiguate the type node as a member of the qualifier type.
	    ClassType ct = n.objectType().type().toClass();

	    // According to JLS2 15.9.1, the class type being
	    // instantiated must be inner.
	    if (! ct.isInnerClass()) {
		throw new SemanticException(
		                            "Cannot provide a containing instance for non-inner class " +
		                            ct.fullName() + ".", n.qualifier().position());
	    }
	}
	else {
	    ClassType ct = n.objectType().type().toClass();

	    if (ct.isMember()) {
		for (ClassType t = ct; t.isMember(); t = t.outer()) {
		    if (! t.flags().isStatic()) {
			throw new SemanticException(
			                            "Cannot allocate non-static member class \"" +
			                            t + "\".", n.position());
		    }
		}
	    }
	}
    }

    private New_c typeCheckerHeader(New_c n, Context context) throws SemanticException {
	n = typeCheckObjectType(context, n);

	Expr qualifier = n.qualifier();
	TypeNode tn = n.objectType();
	List<Expr> arguments = n.arguments();
	ClassBody body = n.body();

	if (body != null) {
	    Ref<? extends Type> ct = tn.typeRef();
	    ClassDef anonType = n.anonType();

	    assert anonType != null;

	    if (!ct.get().toClass().flags().isInterface()) {
		anonType.superType(ct);
	    }
	    else {
		anonType.superType(Types.<Type> ref(ts.Object()));
		assert anonType.interfaces().isEmpty() || anonType.interfaces().get(0) == ct;
		if (anonType.interfaces().isEmpty())
		    anonType.addInterface(ct);
	    }
	}

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

	    if (tn instanceof AmbTypeNode && ((AmbTypeNode) tn).prefix() == null) {
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

		Name name = ((AmbTypeNode) tn).name().id();
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

	if (n.expr() == null) {
	    return n;
	}

	if (!ts.isImplicitCastValid(n.expr().type(), ts.Int(), context) && !ts.isImplicitCastValid(n.expr().type(), ts.Char(), context)) {
	    throw new SemanticException("Case label must be an byte, char, short, or int.", n.position());
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
	return (Block_c) acceptChildren(n);
    }

    public Node visit(Conditional_c n, Context context) throws SemanticException {
	n = (Conditional_c) acceptChildren(n);

	Expr e1 = n.consequent();
	Expr e2 = n.alternative();
	Type t1 = e1.type();
	Type t2 = e2.type();

	if (!n.cond().type().isBoolean()) {
	    throw new SemanticException("Condition of ternary expression must be of type boolean.", n.cond().position());
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
	    if (t1.isByte() && t2.isShort() || t1.isShort() && t2.isByte()) {
		return n.type(ts.Short());
	    }

	    // - If one of the operands is of type T where T is byte, short,
	    // or
	    // char, and the other operand is a constant expression of type
	    // int
	    // whose value is representable in type T, then the type of the
	    // conditional expression is T.

	    if (t1.isIntOrLess() && t2.isInt() && ts.numericConversionValid(t1, e2.constantValue(), context)) {
		return n.type(t1);
	    }

	    if (t2.isIntOrLess() && t1.isInt() && ts.numericConversionValid(t2, e1.constantValue(), context)) {
		return n.type(t2);
	    }

	    // - Otherwise, binary numeric promotion (Sec. 5.6.2) is applied
	    // to the
	    // operand types, and the type of the conditional expression is
	    // the
	    // promoted type of the second and third operands. Note that
	    // binary
	    // numeric promotion performs value set conversion (Sec. 5.1.8).
	    return n.type(ts.promote(t1, t2));
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
		return n.type(t2);
	    }
	    if (ts.isImplicitCastValid(t2, t1, context)) {
		return n.type(t1);
	    }
	}

	throw new SemanticException("Could not determine type of ternary conditional expression; cannot assign " + t1 + " to " + t2 + " or vice versa.",
	                            n.position());
    }

    public Node visit(AmbPrefix_c n, Context context) throws SemanticException {
	n = (AmbPrefix_c) acceptChildren(n);

	Position pos = n.position();
	Node n1 = disamb(n, context, n.prefix(), n.nameNode());
	n1 = copyAttributesFrom(n1, n);

	if (n1 instanceof Prefix) {
	    if (n1 != n)
		n1 = n1.accept(this, context);
	    return n1;
	}
	throw new SemanticException("Could not find " + (n.prefix() != null ? n.prefix() + "." : "") + n.nameNode(), pos);
    }

    public Node visit(AmbExpr_c n, Context context) throws SemanticException {
	n = (AmbExpr_c) acceptChildren(n);

	Position pos = n.position();
	Node n1 = disamb(n, context, null, n.name());
	n1 = copyAttributesFrom(n1, n);

	if (n1 instanceof Expr) {
	    if (n1 != n)
		n1 = n1.accept(this, context);
	    return n1;
	}

	throw new SemanticException("Could not find field or local " + "variable \"" + n.name() + "\".", pos);
    }

    public Node visit(Field_c n, Context context) throws SemanticException {
	n = (Field_c) acceptChildren(n);

	Context c = context;

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

	try {
	    Prefix prefix = n.prefix();
	    Id name = n.name();

	    assert name != null : "null name for " + n;

	    Node n1 = disamb(n, context, prefix, name);
	    n1 = copyAttributesFrom(n1, n);

	    if (n1 instanceof TypeNode) {
		TypeNode tn = (TypeNode) n1;
		LazyRef<Type> sym = (LazyRef<Type>) n.typeRef();
		sym.update(tn.typeRef().get());

		// Reset the resolver goal to one that can run when the ref
		// is deserialized.
		Goal resolver = Globals.Scheduler().LookupGlobalType(sym);
		resolver.update(Goal.Status.SUCCESS);
		sym.setResolver(resolver);


		if (n1 != n)
		    n1 = n1.accept(this, context);

		return n1;
	    }

	    ex = new SemanticException("Could not find type \"" + (n.prefix() == null ? n.name().id() : n.prefix().toString() + "." + n.name().id())
	                               + "\".", n.position());
	}
	catch (SemanticException e) {
	    ex = e;
	}

	// Mark the type as an error, so we don't try looking it up again.
	LazyRef<Type> sym = (LazyRef<Type>) n.typeRef();
	sym.update(this.ts.unknownType(n.position()));

	throw ex;
    }

    private Node disamb(Ambiguous n, Context context, Prefix prefix, Id name) throws SemanticException {
	if (prefix instanceof Ambiguous) {
	    throw new SemanticException(
	    "Cannot disambiguate node with ambiguous prefix.");
	}

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

	assert ! (result instanceof Ambiguous);
	return result;
    }

    private Node disambNoPrefix(Position pos, Prefix prefix, Id name, Ambiguous amb, Context c) throws SemanticException {
	if (exprOk(amb)) {
	    // First try local variables and fields.
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

    private boolean packageOk(Ambiguous amb) {
	return ! (amb instanceof Receiver) &&
	(amb instanceof QualifierNode || amb instanceof Prefix);
    }

    private Node disambVarInstance(VarInstance vi, Position pos, Id name, Context c) throws SemanticException {
	Node n = null;
	if (vi instanceof FieldInstance) {
	    FieldInstance fi = (FieldInstance) vi;
	    Receiver r = makeMissingFieldTarget(c, pos, name, fi);
	    n = nf.Field(pos, r, name).fieldInstance(fi).targetImplicit(true);
	    return n;
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

    private TypeNode makeTypeNode(Type type, Position pos, Ambiguous amb) {
	if (amb instanceof TypeNode) {
	    TypeNode tn = (TypeNode) amb;
	    if (tn.typeRef() instanceof LazyRef) {
		LazyRef<Type> sym = (LazyRef<Type>) tn.typeRef();
		sym.update(type);

		// Reset the resolver goal to one that can run when the ref is deserialized.
		Goal resolver = Globals.Scheduler().LookupGlobalType(sym);
		resolver.update(Goal.Status.SUCCESS);
		sym.setResolver(resolver);

		TypeNode n = nf.CanonicalTypeNode(pos, sym);
		n = (TypeNode) setContext(n, amb.context());
		return n;
	    }
	}

	TypeNode n = nf.CanonicalTypeNode(pos, type);
	n = (TypeNode) setContext(n, amb.context());
	return n;
    }

    private boolean typeOk(Ambiguous amb) {
	return ! (amb instanceof Expr) &&
	(amb instanceof TypeNode || amb instanceof QualifierNode ||
		amb instanceof Receiver || amb instanceof Prefix);
    }

    private boolean exprOk(Ambiguous amb) {
	return ! (amb instanceof QualifierNode) &&
	! (amb instanceof TypeNode) &&
	(amb instanceof Expr || amb instanceof Receiver ||
		amb instanceof Prefix);
    }

    private Node disambExprPrefix(Position pos, Id name, Ambiguous amb, Context c, Expr e) throws SemanticException {
	// Must be a non-static field.
	if (exprOk(amb)) {
	    Node n = nf.Field(pos, e, name);
	    n = setContext(n, c);
	    n = accept(n);
	    return n;
	}
	return null;
    }

    private Node disambTypeNodePrefix(Position pos, Prefix prefix, Id name, Ambiguous amb, Context c, TypeNode tn) throws SemanticException {
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

    private Node disambPackagePrefix(Position pos, Id name, Ambiguous amb, Context c, PackageNode pn) throws SemanticException {
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

	try {
	    Node n1 = disamb(n, context, n.prefix(), n.name());
	    n1 = copyAttributesFrom(n1, n);

	    if (n1 instanceof QualifierNode) {
		QualifierNode qn = (QualifierNode) n1;
		Qualifier q = qn.qualifierRef().get();
		LazyRef<Qualifier> sym = (LazyRef<Qualifier>) n.qualifierRef();
		sym.update(q);

		if (n1 != n)
		    n1 = n1.accept(this, context);

		return n1;
	    }

	    ex = new SemanticException("Could not find type or package \""
	                               + (n.qualifier() == null ? n.name().toString() : n.prefix().toString() + "." + n.name().toString()) + "\".", n.position());
	}
	catch (SemanticException e) {
	    ex = e;
	}

	// Mark the type as an error, so we don't try looking it up again.
	LazyRef<Qualifier> sym = (LazyRef<Qualifier>) n.qualifierRef();
	sym.update(this.ts.unknownQualifier(n.position()));

	throw ex;
    }

    public Node visit(AmbReceiver_c n, Context context) throws SemanticException {
	Node n1 = visit((AmbPrefix_c) n, context);

	if (n1 instanceof Receiver) {
	    return n1;
	}

	throw new SemanticException("Could not find type, field, or " + "local variable \""
	                            + (n.prefix() == null ? n.nameNode().toString() : n.prefix().toString() + "." + n.nameNode().toString()) + "\".", n.position());
    }

    public Node visit(ArrayTypeNode_c n, Context context) throws SemanticException {
	n = (ArrayTypeNode_c) acceptChildren(n);
	CanonicalTypeNode n1 = nf.CanonicalTypeNode(n.position(), ts.arrayOf(n.position(), n.base().typeRef()));
	n1 = (CanonicalTypeNode) copyAttributesFrom(n1, n);
	return n1;
    }

    private Node copyAttributesFrom(Node neu, Node old) {
	if (neu == null)
	    return null;
	Context context = old.context();
	neu = setContext(neu, context);
	return neu;
    }

    private Node setContext(Node n, Context context) {
	if (n == null)
	    return null;

	ContextVisitor v = new ContextVisitor(job, ts, nf) {
	    @Override
	    protected Node leaveCall(Node n) throws SemanticException {
		return n.context(context().freeze());
	    }
	};
	v = v.context(context);
	n = n.visit(v);
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
	return (Try_c) acceptChildren(n);
    }

    public Node visit(Branch_c n, Context context) throws SemanticException {
	return (Branch_c) acceptChildren(n);
    }

    public Node visit(Catch_c n, Context context) throws SemanticException {
	n = (Catch_c) acceptChildren(n);

	if (!n.catchType().isThrowable()) {
	    throw new SemanticException("Can only throw subclasses of \"" + ts.Throwable() + "\".", n.formal().position());

	}

	return n;
    }

    public Node visit(AmbAssign_c n, Context context) throws SemanticException {
	assert n != null;
	assert n.left() != null;
	assert n.operator() != null;
	assert n.right() != null;

	n = (AmbAssign_c) acceptChildren(n);
	
	assert n != null;
	assert n.left() != null;
	assert n.operator() != null;
	assert n.right() != null;

	if (n.left() instanceof Local) {
	    LocalAssign a = nf.LocalAssign(n.position(), (Local) n.left(), n.operator(), n.right());
	    a = (LocalAssign) copyAttributesFrom(a, n);
	    return a.accept(this, context);
	}
	else if (n.left() instanceof Field) {
	    FieldAssign a = nf.FieldAssign(n.position(), ((Field) n.left()).target(), ((Field) n.left()).name(), n.operator(), n.right());
	    a = (FieldAssign) copyAttributesFrom(a, n);
	    a = a.targetImplicit(((Field) n.left()).isTargetImplicit());
	    a = a.fieldInstance(((Field) n.left()).fieldInstance());
	    return a.accept(this, context);
	}
	else if (n.left() instanceof ArrayAccess) {
	    ArrayAccessAssign a = nf.ArrayAccessAssign(n.position(), ((ArrayAccess) n.left()).array(), ((ArrayAccess) n.left()).index(), n.operator(),
	                                               n.right());
	    a = (ArrayAccessAssign) copyAttributesFrom(a, n);
	    return a.accept(this, context);
	}

	// LHS is still ambiguous. The pass should get rerun later.
	return n;
	// throw new
	// SemanticException("Could not disambiguate left side of assignment!",
	// n.position());
    }

    public Node visit(FlagsNode_c n, Context context) throws SemanticException {
	return (FlagsNode_c) acceptChildren(n);
    }

    public Node visit(Id_c n, Context context) throws SemanticException {
	return (Id_c) acceptChildren(n);
    }

    public Node visit(Import_c n, Context context) throws SemanticException {
	n = (Import_c) acceptChildren(n);

	// Make sure the imported name exists.
	if (n.kind() == Import_c.PACKAGE && ts.systemResolver().packageExists(n.name()))
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

	    if (ts.isImplicitCastValid(n.expr().type(), returnType, c)) {
		return n;
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

	if (!n.expr().type().isThrowable()) {
	    throw new SemanticException("Can only throw subclasses of \"" + this.ts.Throwable() + "\".", n.expr().position());
	}

	return n;
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
		typeCheckElements(context, n.type().type(), (ArrayInit) n.init());
	    }
	    else {
		if (!ts.isImplicitCastValid(n.init().type(), n.type().type(), context) && !ts.typeEquals(n.init().type(), n.type().type(), context)
			&& !ts.numericConversionValid(n.type().type(), n.init().constantValue(), context)) {

		    throw new SemanticException("The type of the variable " + "initializer \"" + n.init().type() + "\" does not match that of "
		                                + "the declaration \"" + n.type().type() + "\".", n.init().position());
		}
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
	return (SwitchBlock_c) acceptChildren(n);
    }

    public Node visit(LocalClassDecl_c n, Context context) throws SemanticException {
	return (LocalClassDecl_c) acceptChildren(n);
    }

    public Node visit(IntLit_c n, Context context) throws SemanticException {
	polyglot.ast.IntLit.Kind kind = n.kind();

	if (kind == IntLit_c.INT) {
	    return n.type(ts.Int());
	}
	else if (kind == IntLit_c.LONG) {
	    return n.type(ts.Long());
	}
	else {
	    throw new InternalCompilerError("Unrecognized IntLit kind " + kind);
	}
    }

    public Node visit(FloatLit_c n, Context context) throws SemanticException {
	polyglot.ast.FloatLit.Kind kind = n.kind();

	if (kind == FloatLit_c.FLOAT) {
	    return n.type(ts.Float());
	}
	else if (kind == FloatLit_c.DOUBLE) {
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
	ClassDecl_c n1 = typeCheckSupers(context, n);

	ClassBody body = (ClassBody) accept(n1.body());
	Node n2 = (ClassDecl_c) n1.body(body);

	return n2;
    }

    private ClassDecl_c typeCheckSupers(Context context, ClassDecl_c n) throws SemanticException {
	n.classDef().inStaticContext(context.inStaticContext());

	FlagsNode flags = n.flags();
	Id name = n.name();
	TypeNode superClass = n.superClass();
	List<TypeNode> interfaces = n.interfaces();

	flags = (FlagsNode) accept(n.flags());
	name = (Id) accept(n.name());
	superClass = (TypeNode) accept(n.superClass());
	interfaces = (List<TypeNode>) accept(n.interfaces());

	if (n.superClass() != null)
	    assert n.classDef().superType() == n.superClass().typeRef();

	n = (ClassDecl_c) n.flags(flags);
	n = (ClassDecl_c) n.name(name);
	n = (ClassDecl_c) n.superClass(superClass);
	n = (ClassDecl_c) n.interfaces(interfaces);

	checkSupertypeCycles(n);

	return n;
    }

    private void checkSupertypeCycles(ClassDecl_c n) throws SemanticException {
	Ref<? extends Type> stref = n.classDef().superType();
	if (stref != null) {
	    Type t = stref.get();
	    if (t instanceof UnknownType)
		throw new SemanticException(); // already reported
		if (!t.isClass() || t.toClass().flags().isInterface()) {
		    throw new SemanticException("Cannot extend type " + t + "; not a class.", n.superClass() != null ? n.superClass().position() : n.position());
		}
		ts.checkCycles((ReferenceType) t);
	}

	for (Ref<? extends Type> tref : n.classDef().interfaces()) {
	    Type t = tref.get();
	    //		assert !(t instanceof UnknownType);
	    if (!t.isClass() || !t.toClass().flags().isInterface()) {
		String s = n.classDef().flags().isInterface() ? "extend" : "implement";
		throw new SemanticException("Cannot " + s + " type " + t + "; not an interface.", n.position());
	    }
	    ts.checkCycles((ReferenceType) t);
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
	    throw new SemanticException("Inner classes cannot declare " + "static initializers.", n.position());
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
	    throw new SemanticException(
	                                "Local variable \"" + n.name() + "\" multiply defined.  " + "Previous definition at " + outerLocal.position() + ".",
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
