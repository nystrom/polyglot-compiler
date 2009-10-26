package polyglot.dispatch;

import java.util.*;

import polyglot.ast.*;
import polyglot.frontend.*;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.types.VarDef_c.ConstantValue;
import polyglot.util.*;
import polyglot.visit.*;

public class TypeBuilder extends Visitor {

    Job job;
    TypeSystem ts;
    NodeFactory nf;

    public TypeBuilder(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
    }

    public Node visit(Node_c n, TypeBuilderContext tb) throws SemanticException {
//	System.out.println("missing node " + n + " instanceof " + n.getClass().getName());
	return (Node_c) acceptChildren(n, tb);
    }
    
    public Node visit(SourceFile_c n, TypeBuilderContext tb) throws SemanticException {
        if (n.package_() != null) {
            tb = tb.pushPackage(Types.get(n.package_().package_()));
        }
	return (Node_c) acceptChildren(n, tb);
    }

    public Node visit(Expr_c n, TypeBuilderContext tb) throws SemanticException {
	n = (Expr_c) acceptChildren(n, tb);
	
	final Expr_c n0 = n;
	
	((LazyRef<Type>) n.typeRef()).setResolver(new Runnable() {
	    public void run() {
		n0.checked();
	    } 
	});
	
	return n;
    }

    public Node visit(FieldAssign_c n, TypeBuilderContext tb) throws SemanticException {
	n = (FieldAssign_c) acceptChildren(n, tb);

	final FieldAssign_c n0 = n;
	((LazyRef<FieldInstance>) n.getFi()).setResolver(new Runnable() {
	    public void run() {
		n0.checked();
	    } 
	});

	return visit((Expr_c) n, tb);
    }


    public Node visit(Call_c n, TypeBuilderContext tb) throws SemanticException {
	n = (Call_c) acceptChildren(n, tb);
	n = (Call_c) visit((Expr_c) n, tb);
	
	final Call_c n0 = n;

	((LazyRef<MethodInstance>) n.methodInstanceRef()).setResolver(new Runnable() {
	    public void run() {
		n0.checked();
	    } 
	});

	return n;
    }

    public Node visit(LocalDecl_c n, TypeBuilderContext tb) throws SemanticException {
	n = (LocalDecl_c) acceptChildren(n, tb);
	LocalDef li = ts.localDef(n.position(), n.flags().flags(), n.type().typeRef(), n.name().id());
	return n.localDef(li);
    }

    public Node visit(Local_c n, TypeBuilderContext tb) throws SemanticException {
	n = (Local_c) acceptChildren(n, tb);
	n = (Local_c) visit((Expr_c) n, tb);
	LocalInstance li = ts.createLocalInstance(n.position(), new ErrorRef_c<LocalDef>(ts, n.position(), "Cannot get LocalDef before type-checking local variable."));
	return n.localInstance(li);
    }


    public Node visit(ConstructorCall_c n, TypeBuilderContext tb) throws SemanticException {
	n = (ConstructorCall_c) acceptChildren(n, tb);
	
	// Remove super() calls for java.lang.Object.
	if (n.kind() == ConstructorCall_c.SUPER && tb.currentClass() == ts.Object()) {
	    return nf.Empty(n.position());
	}

	final ConstructorCall_c n0 = n;
	((LazyRef<ConstructorInstance>) n.getCi()).setResolver(new Runnable() {
	    public void run() {
		n0.checked();
	    } 
	});

	return n;
    }

    public Node visit(New_c n, TypeBuilderContext tb) throws SemanticException {
	final New_c n0 = n;
	((LazyRef<ConstructorInstance>) n.getCi()).setResolver(new Runnable() {
	    public void run() {
		n0.checked();
	    } 
	});

	Expr qual = (Expr) accept(n.qualifier(), tb);
	TypeNode objectType = (TypeNode) accept(n.objectType(), tb);
	List<Expr> arguments = (List<Expr>) accept(n.arguments(), tb);

	ClassBody body = null;

	if (n.body() != null) {
	    TypeBuilderContext tb2 = tb.pushAnonClass(n.position());
	    ClassDef type = tb2.currentClass();

	    n = (New_c) n.anonType(type);

	    body = (ClassBody) accept(n.body(), tb2);
	}

	n = (New_c) n.qualifier(qual);
	n = (New_c) n.objectType(objectType);
	n = (New_c) n.arguments(arguments);
	n = (New_c) n.body(body);

	return visit((Expr_c) n, tb);
    }

    public Node visit(Field_c n, TypeBuilderContext tb) throws SemanticException {
	n = (Field_c) acceptChildren(n, tb);
	n = (Field_c) visit((Expr_c) n, tb);

	final Field_c n0 = n;
	((LazyRef<FieldInstance>) n.getFi()).setResolver(new Runnable() {
	    public void run() {
		n0.checked();
	    } 
	});

	return n;
    }

    public Node visit(AmbTypeNode_c n, TypeBuilderContext tb) throws SemanticException {
	n = (AmbTypeNode_c) acceptChildren(n, tb);
	if (n.typeRef() == null) {
	    LazyRef<UnknownType> r = Types.lazyRef(ts.unknownType(n.position()));
	    r.setResolver(new TypeCheckTypeGoal(n, tb.job(), ts, nf, r, false));
	    return n.typeRef(r);
	}
	else {
	    return n;
	}
    }

    public Node visit(AmbQualifierNode_c n, TypeBuilderContext tb) throws SemanticException {
	n = (AmbQualifierNode_c) acceptChildren(n, tb);
	LazyRef<Qualifier> sym = Types.<Qualifier>lazyRef(ts.unknownQualifier(n.position()), new SetResolverGoal(tb.job()));
	sym.setResolver(new TypeCheckTypeGoal(n, tb.job(), ts, nf, sym, false));
	return n.qualifier(sym);
    }

    public Node visit(AmbReceiver_c n, TypeBuilderContext tb) throws SemanticException {
	n = (AmbReceiver_c) acceptChildren(n, tb);
	return n.type(new TypeBuilderContext(job, ts, nf).typeSystem().unknownType(n.position()));
    }

    public Node visit(ArrayTypeNode_c n, TypeBuilderContext tb) throws SemanticException {
	n = (ArrayTypeNode_c) acceptChildren(n, tb);
	return n.typeRef(Types.<Type>ref(new TypeBuilderContext(job, ts, nf).typeSystem().arrayOf(n.position(), n.base().typeRef())));
    }

    public Node visit(CanonicalTypeNode_c n, TypeBuilderContext tb) throws SemanticException {
	n = (CanonicalTypeNode_c) acceptChildren(n, tb);
	if (n.type() == null) {
	    LazyRef<UnknownType> r = Types.lazyRef(ts.unknownType(n.position()));
	    r.setResolver(new TypeCheckTypeGoal(n, tb.job(), ts, nf, r, false));
	    return n.typeRef(r);
	}
	else {
	    return n;
	}
    }

    public Node visit(MethodDecl_c n, TypeBuilderContext tb) throws SemanticException {
	ClassDef ct = tb.currentClass();
	assert ct != null;

	Flags flags = n.flags().flags();

	if (ct.flags().isInterface()) {
	    flags = flags.Public().Abstract();
	}

	MethodDef mi = n.createMethodDef(ts, ct, flags);
	ct.addMethod(mi);

	TypeBuilderContext tbChk = tb.pushCode(mi);

	final TypeBuilderContext tbx = tb;
	final MethodDef mix = mi;

	MethodDecl_c n1 = (MethodDecl_c) n.visitSignature(new NodeVisitor() {
	    public Node override(Node n) {
		return accept(n, tbx.pushCode(mix));
	    }
	});

	List<Ref<? extends Type>> formalTypes = new ArrayList<Ref<? extends Type>>(n1.formals().size());
	for (Formal f : n1.formals()) {
	    formalTypes.add(f.type().typeRef());
	}

	List<Ref<? extends Type>> throwTypes = new ArrayList<Ref<? extends Type>>(n1.throwTypes().size());
	for (TypeNode tn : n1.throwTypes()) {
	    throwTypes.add(tn.typeRef());
	}

	mi.setReturnType(n1.returnType().typeRef());
	mi.setFormalTypes(formalTypes);
	mi.setThrowTypes(throwTypes);

	Block body = (Block) accept(n1.body(), tbChk);

	n1 = (MethodDecl_c) n1.body(body);
	return n1.methodDef(mi);
    }

    public Node visit(FieldDecl_c n, TypeBuilderContext tb) throws SemanticException {
	ClassDef ct = tb.currentClass();
	assert ct != null;

	Flags flags = n.flags().flags();

	if (ct.flags().isInterface()) {
	    flags = flags.Public().Static().Final();
	}

	FieldDef fi = n.createFieldDef(ts, ct, flags);
	ct.addField(fi);

	TypeBuilderContext tbChk = tb.pushDef(fi);

	InitializerDef ii = null;

	if (n.init() != null) {
	    Flags iflags = flags.isStatic() ? Flags.STATIC : Flags.NONE;
	    ii = n.createInitializerDef(ts, ct, iflags);
	    fi.setInitializer(ii);
	    tbChk = tbChk.pushCode(ii);
	}

	final TypeBuilderContext tbx = tb;
	final FieldDef mix = fi;

	final FieldDecl_c n0 = n;
	FieldDecl_c n1 = (FieldDecl_c) n.visitSignature(new NodeVisitor() {
	    public Node override(Node n) {
		return accept(n, tbx.pushDef(mix));
	    }
	});

	fi.setType(n1.type().typeRef());

	Expr init = (Expr) accept(n1.init(), tbChk);
	n1 = (FieldDecl_c) n1.init(init);

	n1 = (FieldDecl_c) n1.fieldDef(fi);

	if (ii != null) {
	    n1 = (FieldDecl_c) n1.initializerDef(ii);
	}

	n1 = (FieldDecl_c) n1.flags(n1.flags().flags(flags));

	final Node xx = n1;

	final FieldDef def = n1.fieldDef();
	Ref<ConstantValue> r = def.constantValueRef();
	if (r instanceof LazyRef) {
	    ((LazyRef<ConstantValue>) r).setResolver(new AbstractGoal_c("ConstantValue") {
		public boolean runTask() {
		    if (state() == Goal.Status.RUNNING_RECURSIVE || state() == Goal.Status.RUNNING_WILL_FAIL) {
			// The field is not constant if the initializer is recursive.
			//
			// But, we could be checking if the field is constant for another
			// reference in the same file:
			//
			// m() { use x; }
			// final int x = 1;
			//
			// So this is incorrect.  The goal below needs to be refined to only visit the initializer.
			def.setNotConstant();
		    }
		    else {
			xx.checked();
		    }
		    return true;
		}
	    });
	}

	return n1;
    }

    public Node visit(ConstructorDecl_c n, TypeBuilderContext tb) throws SemanticException {
	ClassDef ct = tb.currentClass();
	assert ct != null;

	Flags flags = n.flags().flags();

	if (ct.flags().isInterface()) {
	    flags = flags.Public().Abstract();
	}

	ConstructorDef ci = n.createConstructorDef(ts, ct, flags);
	ct.addConstructor(ci);

	TypeBuilderContext tbChk = tb.pushCode(ci);

	final TypeBuilderContext tbx = tb;
	final ConstructorDef mix = ci;

	ConstructorDecl_c n1 = (ConstructorDecl_c) n.visitSignature(new NodeVisitor() {
	    public Node override(Node n) {
		return accept(n, tbx.pushCode(mix));
	    }
	});

	List<Ref<? extends Type>> formalTypes = new ArrayList<Ref<? extends Type>>(n1.formals().size());
	for (Formal f : n1.formals()) {
	    formalTypes.add(f.type().typeRef());
	}

	List<Ref<? extends Type>> throwTypes = new ArrayList<Ref<? extends Type>>(n1.throwTypes().size());
	for (TypeNode tn : n1.throwTypes()) {
	    throwTypes.add(tn.typeRef());
	}

	ci.setFormalTypes(formalTypes);
	ci.setThrowTypes(throwTypes);

	Block body = (Block) accept(n1.body(), tbChk);

	n1 = (ConstructorDecl_c) n1.body(body);
	return n1.constructorDef(ci);
    }

    public Node visit(ClassDecl_c n, TypeBuilderContext tb) throws SemanticException {
	n = preBuildTypes(tb, n);
	n = buildTypesBody(tb, n);
	n = postBuildTypes(tb, n);
	return n;
    }

    private ClassDecl_c postBuildTypes(TypeBuilderContext tb, ClassDecl_c n1) throws SemanticException {
	ClassDecl_c n = (ClassDecl_c) n1.copy();

	if (defaultConstructorNeeded(n)) {
	    ConstructorDecl cd = createDefaultConstructor(n);
	    TypeBuilderContext tb2 = tb.pushClass(n.classDef());
	    cd = (ConstructorDecl) accept(cd, tb2);
	    n = (ClassDecl_c) n.body(n.body().addMember(cd));
	}

	return n;
    }

    private ConstructorDecl createDefaultConstructor(ClassDecl_c n) throws SemanticException {
	Position pos = n.body().position().startOf();
	
	Block block = null;
	
	Ref<? extends Type> superType = n.classDef().superType();
	
	if (superType != null) {
	    ConstructorCall cc = nf.SuperCall(pos, Collections.EMPTY_LIST);
	    block = nf.Block(pos, cc);
	}
	else {
	    block = nf.Block(pos);
	}
	
	ConstructorDecl cd = nf.ConstructorDecl(pos, nf.FlagsNode(pos, Flags.PUBLIC), n.name(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, block);
	return cd;
    }

    private boolean defaultConstructorNeeded(ClassDecl_c n) {
	if (n.flags().flags().isInterface()) {
	    return false;
	}
	for (ClassMember cm : n.body().members()) {
	    if (cm instanceof ConstructorDecl) {
	        return false;
	    }
	}
	
	return true;
    }

    private ClassDecl_c buildTypesBody(TypeBuilderContext tb, ClassDecl_c n) throws SemanticException {
	TypeBuilderContext tb2 = tb.pushClass(n.classDef());
	ClassBody body = (ClassBody) accept(n.body(), tb2);
	n = (ClassDecl_c) n.body(body);
	return n;
    }

    private ClassDecl_c preBuildTypes(TypeBuilderContext tb, ClassDecl_c n1) throws SemanticException {
	tb = tb.pushClass(n1.position(), n1.flags().flags(), n1.name().id());

	ClassDef type = tb.currentClass();

	// Member classes of interfaces are implicitly public and static.
	if (type.isMember() && type.outer().get().flags().isInterface()) {
	    type.flags(type.flags().Public().Static());
	}

	// Member interfaces are implicitly static. 
	if (type.isMember() && type.flags().isInterface()) {
	    type.flags(type.flags().Static());
	}

	// Interfaces are implicitly abstract. 
	if (type.flags().isInterface()) {
	    type.flags(type.flags().Abstract());
	}

	ClassDecl_c n = n1;
	FlagsNode flags = (FlagsNode) accept(n.flags(), tb);
	Id name = (Id) accept(n.name(), tb);

	TypeNode superClass = (TypeNode) accept(n.superClass(), tb);
	List<TypeNode> interfaces = (List<TypeNode>) accept(n.interfaces(), tb);

	n = (ClassDecl_c) n.flags(flags);
	n = (ClassDecl_c) n.name(name);
	n = (ClassDecl_c) n.superClass(superClass);
	n = (ClassDecl_c) n.interfaces(interfaces);

	n = (ClassDecl_c) n.classDef(type).flags(flags.flags(type.flags()));

	setSuperclass(n);
	setInterfaces(n);

	return n;
    }

    private void setInterfaces(ClassDecl_c n) throws SemanticException {
	ClassDef cd = 
	n.classDef();
	List<TypeNode> interfaces = n.interfaces();
	for (TypeNode tn : interfaces) {
	    Ref<? extends Type> t = tn.typeRef();
	
	    if (Report.should_report(Report.types, 3))
	        Report.report(3, "adding interface of " + cd + " to " + t);
	
	    cd.addInterface(t);
	}
    }

    private void setSuperclass(ClassDecl_c n) throws SemanticException {
	ClassDef cd = n.classDef();
	TypeNode superClass = n.superClass();
	
	QName objectName = ((ClassType) ts.Object()).fullName();
	
	if (superClass != null) {
	    Ref<? extends Type> t = superClass.typeRef();
	    if (Report.should_report(Report.types, 3))
	        Report.report(3, "setting superclass of " + n.classDef() + " to " + t);
	    cd.superType(t);
	}
	else if (cd.asType().equals((Object) ts.Object()) || cd.fullName().equals(objectName)) {
	    // the type is the same as ts.Object(), so it has no superclass.
	    if (Report.should_report(Report.types, 3))
	        Report.report(3, "setting superclass of " + n.classDef() + " to " + null);
	    cd.superType(null);
	}
	else {
	    // the superclass was not specified, and the type is not the same
	    // as ts.Object() (which is typically java.lang.Object)
	    // As such, the default superclass is ts.Object().
	    if (Report.should_report(Report.types, 3))
	        Report.report(3, "setting superclass of " + n.classDef() + " to " + ts.Object());
	    cd.superType(Types.<Type>ref(ts.Object()));
	}
    }

    public Node visit(Initializer_c n, final TypeBuilderContext tb) throws SemanticException {
	ClassDef ct = tb.currentClass();
	assert ct != null;

	Flags flags = n.flags().flags();

	InitializerDef ii = n.createInitializerDef(ts, ct, flags);
	TypeBuilderContext tbChk = tb.pushCode(ii);

	final InitializerDef mix = ii;

	n = (Initializer_c) n.visitSignature(new NodeVisitor() {
	    public Node override(Node n) {
		return accept(n, tb.pushCode(mix));
	    }
	});

	Block body = (Block) accept(n.body(), tbChk);
	n = (Initializer_c) n.body(body);

	n = (Initializer_c) n.initializerDef(ii);

	return n;
    }

    public Node visit(Formal_c n, TypeBuilderContext tb) throws SemanticException {
	n = (Formal_c) visit((Node_c) n, tb);
	
	LocalDef li = ts.localDef(n.position(), n.flags().flags(), n.type().typeRef(), n.name().id());

	// Formal parameters are never compile-time constants.
	li.setNotConstant();

	return n.localDef(li);
    }
}
