package polyglot.dispatch;

import java.util.ArrayList;
import java.util.List;

import polyglot.ast.AmbExpr_c;
import polyglot.ast.AmbTypeNode;
import polyglot.ast.ArrayAccess_c;
import polyglot.ast.ArrayInit_c;
import polyglot.ast.Assign;
import polyglot.ast.Assign_c;
import polyglot.ast.Binary_c;
import polyglot.ast.BooleanLit_c;
import polyglot.ast.Call_c;
import polyglot.ast.Cast_c;
import polyglot.ast.CharLit_c;
import polyglot.ast.ClassBody;
import polyglot.ast.ClassLit_c;
import polyglot.ast.Conditional_c;
import polyglot.ast.Expr;
import polyglot.ast.Field_c;
import polyglot.ast.FloatLit_c;
import polyglot.ast.Id;
import polyglot.ast.Instanceof_c;
import polyglot.ast.IntLit_c;
import polyglot.ast.Local_c;
import polyglot.ast.New;
import polyglot.ast.NewArray_c;
import polyglot.ast.New_c;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Node_c;
import polyglot.ast.NullLit_c;
import polyglot.ast.PackageNode;
import polyglot.ast.QualifiedName;
import polyglot.ast.QualifierNode;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.ast.Special_c;
import polyglot.ast.StringLit_c;
import polyglot.ast.TypeNode;
import polyglot.ast.Unary_c;
import polyglot.frontend.Job;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.LocalInstance;
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
import funicular.Clock;

public class NewTypeChecker extends Visitor {

    Job job;
    TypeSystem ts;
    NodeFactory nf;
    
    Clock jobClock() {
        return (Clock) job.get("clock");
    }

    // installation:
    // Every Expr node has a type ref
    // typeRef.setResolver(new Callable<Type>() {
    //    public Type call() {
    //       return n.accept(new NewTypeChecker());
    //    }
    // });
    
    
    // checked version of a node is constructed from the checked version of its children
    // type of node is constructed from type of children

    public NewTypeChecker(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
    }

    @Override
    public Node accept(Node n, Object... args) {
	return n.checked();
    }

    public Type visit(Node_c n, Context context) throws SemanticException {
	System.out.println("missing node " + n + " instanceof " + n.getClass().getName());
	return ts.Void();
    }

    public Type visit(NewArray_c n, Context context) throws SemanticException {
	Type type = ts.arrayOf(n.baseType().type(), n.dims().size() + n.additionalDims());
	return type;
    }

    public Type visit(Assign_c n, Context context) throws SemanticException {
	n = (Assign_c) acceptChildren(n);

	Type t = n.left().type();

	if (t == null)
	    t = ts.unknownType(n.position());

	Expr right = n.right();
	Assign.Operator op = n.operator();

	Type s = right.type();

	if (op == Assign_c.ASSIGN) {
	    return t;
	}

	if (op == Assign_c.ADD_ASSIGN) {
	    // t += s
	    if (ts.typeEquals(t, ts.String(), context) && ts.canCoerceToString(s, context)) {
		return ts.String();
	    }

	    if (t.isNumeric() && s.isNumeric()) {
		return ts.promote(t, s);
	    }

	    throw new SemanticException("The " + op + " operator must have " + "numeric or String operands.", n.position());
	}

	if (op == Assign_c.SUB_ASSIGN || op == Assign_c.MUL_ASSIGN || op == Assign_c.DIV_ASSIGN || op == Assign_c.MOD_ASSIGN) {
	    if (t.isNumeric() && s.isNumeric()) {
		return ts.promote(t, s);
	    }
	}

	if (op == Assign_c.BIT_AND_ASSIGN || op == Assign_c.BIT_OR_ASSIGN || op == Assign_c.BIT_XOR_ASSIGN) {
	    if (t.isBoolean() && s.isBoolean()) {
		return ts.Boolean();
	    }

	    return ts.promote(t, s);
	}

	if (op == Assign_c.SHL_ASSIGN || op == Assign_c.SHR_ASSIGN || op == Assign_c.USHR_ASSIGN) {
		// Only promote the left of a shift.
		return ts.promote(t);
	}

	throw new SemanticException("Unrecognized assignment operator " + op + ".");
    }

    public Type visit(ArrayAccess_c n, Context context) throws SemanticException {
	if (!n.array().type().isArray()) {
	    throw new SemanticException("Subscript can only follow an array type.", n.position());
	}

	return n.array().type().toArray().base();
    }

    public Type visit(ArrayInit_c n, Context context) throws SemanticException {
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
	    return ts.Null(); // should be EmptyArray type
	}
	else {
	    return ts.arrayOf(type);
	}
    }

    public Type visit(Binary_c n, Context context) throws SemanticException {

	if (n.operator() == Binary_c.GT || n.operator() == Binary_c.LT || n.operator() == Binary_c.GE || n.operator() == Binary_c.LE) {
	    return ts.Boolean();
	}

	if (n.operator() == Binary_c.EQ || n.operator() == Binary_c.NE) {
	    return ts.Boolean();
	}

	if (n.operator() == Binary_c.COND_OR || n.operator() == Binary_c.COND_AND) {
	    return ts.Boolean();
	}

	if (n.operator() == Binary_c.ADD) {
	    Type l = n.left().type();
	    Type r = n.right().type();
	    if (ts.isSubtype(l, ts.String(), context) || ts.isSubtype(r, ts.String(), context)) {
		return ts.String();
	    }
	}

	if (n.operator() == Binary_c.BIT_AND || n.operator() == Binary_c.BIT_OR || n.operator() == Binary_c.BIT_XOR) {
	    Type l = n.left().type();
	    Type r = n.right().type();
	    if (l.isBoolean() && r.isBoolean()) {
		return ts.Boolean();
	    }
	}

	if (n.operator() == Binary_c.SHL || n.operator() == Binary_c.SHR || n.operator() == Binary_c.USHR) {
	    // For shift, only promote the left operand.
	    Type l = n.left().type();
	    return ts.promote(l);
	}

	Type l = n.left().type();
	Type r = n.right().type();

	return ts.promote(l, r);
    }

    public Type visit(Unary_c n, Context context) throws SemanticException {
	if (n.operator() == Unary_c.POST_INC || n.operator() == Unary_c.POST_DEC || n.operator() == Unary_c.PRE_INC || n.operator() == Unary_c.PRE_DEC) {
	    return n.expr().type();
	}

	if (n.operator() == Unary_c.BIT_NOT) {
	    return ts.promote(n.expr().type());
	}

	if (n.operator() == Unary_c.NEG || n.operator() == Unary_c.POS) {
	    return ts.promote(n.expr().type());
	}

	if (n.operator() == Unary_c.NOT) {
	    return n.expr().type();
	}

	throw new SemanticException("Could not compute type.", n.position());
    }

    public class MethodInstanceResolver {
	public MethodInstance visit(Call_c n, Context context) throws SemanticException {
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

	    return mi;
	}

	private MethodInstance typeCheckNullTarget(Call_c n, Context context, List<Type> argTypes) throws SemanticException {
	    // the target is null, and thus implicit
	    // let's find the target, using the context, and
	    // set the target appropriately, and then type check
	    // the result
	    MethodInstance mi = context.findMethod(ts.MethodMatcher(null, n.name().id(), argTypes, context));
	    return mi;
	}

    }


    public Type visit(Cast_c n, Context context) throws SemanticException {
	return n.castType().type();
    }

    static class LocalInstanceResolver {
	public LocalInstance visit(Local_c n, Context context) throws SemanticException {
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

	    return li;
	}
    }

    public Type visit(Local_c n, Context context) throws SemanticException {
	LocalInstance li = n.accept(new LocalInstanceResolver(), context);
	return li.type();
    }

    public Type visit(Special_c n, Context context) throws SemanticException {
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
	    return t;
	}
	else if (n.kind() == Special_c.SUPER) {
	    return t.superClass();
	}

	return ts.Void();
    }

    public Type visit(New_c n, Context context) throws SemanticException {
	New_c n1 = typeCheckerHeader(n, context);

	ClassBody body = (ClassBody) accept(n1.body());
	n1 = (New_c) n1.body(body);

	n1 = (New_c) tcNew(n, n1, context);
	return n1.type();
    }

    public Node tcNew(New_c old, New_c n, Context context) throws SemanticException {

	List<Type> argTypes = new ArrayList<Type>(n.arguments().size());

	for (Expr e : n.arguments()) {
	    argTypes.add(e.type());
	}

	typeCheckFlags(n);
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

    private New_c typeCheckerHeader(New_c n, Context context) throws SemanticException {
	n = typeCheckObjectType(context, n);

	Expr qualifier = n.qualifier();
	TypeNode tn = n.objectType();
	List<Expr> arguments = n.arguments();
	ClassBody body = n.body();

	if (body != null) {
	    Ref< Type> ct = tn.typeRef();
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
	    
	    if (tn instanceof AmbTypeNode && ((AmbTypeNode) tn).child() instanceof Id) {
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

		Name name = ((Id) ((AmbTypeNode) tn).child()).id();
		assert name != null;

		if (!qualifier.type().isClass()) {
		    throw new SemanticException("Cannot instantiate member class of non-class type.", n.position());
		}
		Type ct = ts.findMemberType(qualifier.type(), name, c);
		((Ref<Type>) tn.typeRef()).update(ct);
		tn = nf.CanonicalTypeNode(n.objectType().position(), tn.typeRef());
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

	q = (Expr) accept(q);

	New k = n.qualifier(q);
	return k;
    }

    public Type visit(Conditional_c n, Context context) throws SemanticException {
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
	    return t1;
	}

	// Otherwise, if the second and third operands have numeric type,
	// then
	// there are several cases:
	if (t1.isNumeric() && t2.isNumeric()) {
	    // - If one of the operands is of type byte and the other is of
	    // type short, then the type of the conditional expression is
	    // short.
	    if (t1.isByte() && t2.isShort() || t1.isShort() && t2.isByte()) {
		return ts.Short();
	    }

	    // - If one of the operands is of type T where T is byte, short,
	    // or
	    // char, and the other operand is a constant expression of type
	    // int
	    // whose value is representable in type T, then the type of the
	    // conditional expression is T.

	    if (t1.isIntOrLess() && t2.isInt() && ts.numericConversionValid(t1, e2.constantValue(), context)) {
		return t1;
	    }

	    if (t2.isIntOrLess() && t1.isInt() && ts.numericConversionValid(t2, e1.constantValue(), context)) {
		return t2;
	    }

	    // - Otherwise, binary numeric promotion (Sec. 5.6.2) is applied
	    // to the
	    // operand types, and the type of the conditional expression is
	    // the
	    // promoted type of the second and third operands. Note that
	    // binary
	    // numeric promotion performs value set conversion (Sec. 5.1.8).
	    return ts.promote(t1, t2);
	}

	// If one of the second and third operands is of the null type and
	// the
	// type of the other is a reference type, then the type of the
	// conditional expression is that reference type.
	if (t1.isNull() && t2.isReference())
	    return t2;
	if (t2.isNull() && t1.isReference())
	    return t1;

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
		return t2;
	    }
	    if (ts.isImplicitCastValid(t2, t1, context)) {
		return t1;
	    }
	}

	throw new SemanticException("Could not determine type of ternary conditional expression; cannot assign " + t1 + " to " + t2 + " or vice versa.",
	                            n.position());
    }
    
    public Type visit(AmbExpr_c n, Context context) throws SemanticException {
	n = (AmbExpr_c) acceptChildren(n, context);

	if (n.child() instanceof Expr) {
	    return ((Expr) n.child()).type();
	}

	throw new SemanticException("Could not find field or local " + "variable \"" + n.child() + "\".", n.position());
    }

    public class FieldInstanceResolver {
	public FieldInstance visit(Field_c n, Context context) throws SemanticException {
	    Context c = context;

	    FieldInstance fi = ts.findField(n.target().type(), ts.FieldMatcher(n.target().type(), n.name().id(), c));

	    if (fi == null) {
		throw new InternalCompilerError("Cannot access field on node of type " + n.target().getClass().getName() + ".");
	    }

	    return fi;
	}
    }

    public Type visit(Field_c n, Context context) throws SemanticException {
	FieldInstance fi = n.accept(new FieldInstanceResolver(), context);
	return fi.type();
    }

    private Node disamb(Node n, Context context, Node prefix, Id name) throws SemanticException {
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

    private Node disambNoPrefix(Position pos, Node prefix, Id name, Node amb, Context c) throws SemanticException {
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
		pn = (PackageNode) accept(pn);
		return pn;
	    }
	    catch (SemanticException e) {
	    }
	    Package p = ts.createPackage(QName.make(null, name.id()));
	    PackageNode pn = nf.PackageNode(pos, Types.ref(p));
	    pn = (PackageNode) accept(pn);
	    return pn;
	}

	return null;
    }

    private boolean packageOk(Node amb) {
	return ! (amb instanceof Receiver) &&
	(amb instanceof QualifierNode || amb instanceof QualifiedName);
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

	r = (Receiver) accept(r);

	return r;
    }

    private TypeNode makeTypeNode(Type type, final Position pos, Node amb) {
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
	    }, jobClock());

	    TypeNode n = nf.CanonicalTypeNode(pos, sym);
	    return n;
	}

	TypeNode n = nf.CanonicalTypeNode(pos, type);
	return n;
    }

    private boolean typeOk(Node amb) {
	return ! (amb instanceof Expr) &&
	(amb instanceof TypeNode || amb instanceof QualifierNode ||
		amb instanceof Receiver || amb instanceof QualifiedName);
    }

    private boolean exprOk(Node amb) {
	return ! (amb instanceof QualifierNode) &&
	! (amb instanceof TypeNode) &&
	(amb instanceof Expr || amb instanceof Receiver ||
		amb instanceof QualifiedName);
    }

    private Node disambExprPrefix(Position pos, Id name, Node amb, Context c, Expr e) throws SemanticException {
	// Must be a non-static field.
	if (exprOk(amb)) {
	    Node n = nf.Field(pos, e, name);
	    n = accept(n);
	    return n;
	}
	return null;
    }

    private Node disambTypeNodePrefix(Position pos, Node prefix, Id name, Node amb, Context c, TypeNode tn) throws SemanticException {
	// Try static fields.
	Type t = tn.type();

	if (exprOk(amb)) {
	    try {
		FieldInstance fi = ts.findField(t, ts.FieldMatcher(t, name.id(), c));
		Node n = nf.Field(pos, tn, name).fieldInstance(fi);
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

    private Node disambPackagePrefix(Position pos, Id name, Node amb, Context c, PackageNode pn) throws SemanticException {
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
	    n1 = accept(n1);
	    return n1;
	}
	else if (q.isType() && typeOk(amb)) {
	    return makeTypeNode(q.toType(), pos, amb);
	}

	return null;
    }

    public Type visit(IntLit_c n, Context context) throws SemanticException {
	polyglot.ast.IntLit.Kind kind = n.kind();

	if (kind == IntLit_c.INT) {
	    return ts.Int();
	}
	else if (kind == IntLit_c.LONG) {
	    return ts.Long();
	}
	else {
	    throw new InternalCompilerError("Unrecognized IntLit kind " + kind);
	}
    }

    public Type visit(FloatLit_c n, Context context) throws SemanticException {
	polyglot.ast.FloatLit.Kind kind = n.kind();

	if (kind == FloatLit_c.FLOAT) {
	    return ts.Float();
	}
	else if (kind == FloatLit_c.DOUBLE) {
	    return ts.Double();
	}
	else {
	    throw new InternalCompilerError("Unrecognized FloatLit kind " + kind);
	}
    }

    public Type visit(StringLit_c n, Context context) throws SemanticException {
	return ts.String();
    }

    public Type visit(CharLit_c n, Context context) throws SemanticException {
	return ts.Char();
    }

    public Type visit(BooleanLit_c n, Context context) throws SemanticException {
	return ts.Boolean();
    }

    public Type visit(NullLit_c n, Context context) throws SemanticException {
	return ts.Null();
    }

    public Type visit(ClassLit_c n, Context context) throws SemanticException {
	return ts.Class();
    }

    public Type visit(Instanceof_c n, Context context) throws SemanticException {
	return ts.Boolean();
    }
}
