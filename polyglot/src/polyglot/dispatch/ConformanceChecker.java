package polyglot.dispatch;

import java.util.ArrayList;
import java.util.Iterator;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.*;

public class ConformanceChecker extends Visitor {
    Job job;
    NodeFactory nf;
    TypeSystem ts;

    public ConformanceChecker(Job job, TypeSystem ts, NodeFactory nf) {
	this.job = job;
	this.ts = ts;
	this.nf = nf;
    }

    public Node visit(Node n) throws SemanticException {
	return n;
    }
    
    public Node visit(ClassBody_c n) throws SemanticException {
	duplicateFieldCheck(n);
	duplicateConstructorCheck(n);
	duplicateMethodCheck(n);
	duplicateMemberClassCheck(n);
	return n;
    }
    
    private void duplicateMethodCheck(ClassBody_c n) throws SemanticException {
	ClassDef type = n.context().currentClassDef();
	
	ArrayList<MethodDef> l = new ArrayList<MethodDef>(type.methods());
	
	for (int i = 0; i < l.size(); i++) {
	    MethodDef mi = l.get(i);
	    MethodInstance ti = mi.asInstance();
	
	    for (int j = i+1; j < l.size(); j++) {
	        MethodDef mj = l.get(j);
	        MethodInstance tj = mj.asInstance();
	
	        if (ti.isSameMethod(tj, n.context())) {
	            throw new SemanticException("Duplicate method \"" + mj + "\".", mj.position());
	        }
	    }
	}
    }

    private void duplicateMemberClassCheck(ClassBody_c n) throws SemanticException {
	ClassDef type = n.context().currentClassDef();
	ArrayList<Ref<? extends Type>> l = new ArrayList<Ref<? extends Type>>(type.memberClasses());
	
	for (int i = 0; i < l.size(); i++) {
	    Type mi = l.get(i).get();
	
	    for (int j = i+1; j < l.size(); j++) {
	        Type mj = l.get(j).get();
	
	        if (mi instanceof Named && mj instanceof Named) {
	            if (((Named) mi).name().equals(((Named) mj).name())) {
	                throw new SemanticException("Duplicate member type \"" + mj + "\".", mj.position());
	            }
	        }
	    }
	}
    }

    private void duplicateConstructorCheck(ClassBody_c n) throws SemanticException {
	ClassDef type = n.context().currentClassDef();
	ArrayList<ConstructorDef> l = new ArrayList<ConstructorDef>(type.constructors());
	
	for (int i = 0; i < l.size(); i++) {
	    ConstructorDef ci = l.get(i);
	    ConstructorInstance ti = ci.asInstance();
	
	    for (int j = i+1; j < l.size(); j++) {
	        ConstructorDef cj = l.get(j);
	        ConstructorInstance tj = cj.asInstance();
	
	        if (ti.hasFormals(tj.formalTypes(), n.context())) {
	            throw new SemanticException("Duplicate constructor \"" + cj + "\".", cj.position());
	        }
	    }
	}
    }

    private void duplicateFieldCheck(ClassBody_c n) throws SemanticException {
	ClassDef type = n.context().currentClassDef();
	ArrayList<FieldDef> l = new ArrayList<FieldDef>(type.fields());
	
	for (int i = 0; i < l.size(); i++) {
	    FieldDef fi = (FieldDef) l.get(i);
	
	    for (int j = i+1; j < l.size(); j++) {
	        FieldDef fj = (FieldDef) l.get(j);
	
	        if (fi.name().equals(fj.name())) {
	            throw new SemanticException("Duplicate field \"" + fj + "\".", fj.position());
	        }
	    }
	}
    }

    public Node visit(ClassDecl_c n) throws SemanticException {
	ClassType type = n.classDef().asType();
	Name name = n.name().id();
	
	// The class cannot have the same simple name as any enclosing class.
	if (type.isNested()) {
	    ClassType container = type.outer();
	
	    while (container != null) {
	        if (!container.isAnonymous()) {
	            Name cname = container.name();
	
	            if (cname.equals(name)) {
	                throw new SemanticException("Cannot declare member " +
	                        "class \"" + type.fullName() +
	                        "\" inside class with the " +
	                        "same name.", n.position());
	            }
	        }
	        if (container.isNested()) {
	            container = container.outer();
	        }
	        else {
	            break;
	        }
	    }
	}
	
	// A local class name cannot be redeclared within the same
	// method, constructor or initializer, and within its scope                
	if (type.isLocal()) {
	    Context ctxt = n.context();

	    if (ctxt.isLocal(name)) {
	        // Something with the same name was declared locally.
	        // (but not in an enclosing class)                                    
	        Named nm = ctxt.find(ts.TypeMatcher(name));
	        if (nm instanceof Type) {
	            Type another = (Type) nm;
	            if (another.isClass() && another.toClass().isLocal()) {
	                throw new SemanticException("Cannot declare local " +
	                        "class \"" + n.classDef().name() + "\" within the same " +
	                        "method, constructor or initializer as another " +
	                        "local class of the same name.", n.position());
	            }
	        }
	    }                
	}
	
	// check that inner classes do not declare member interfaces
	if (type.isMember() && type.flags().isInterface() &&
	        type.outer().isInnerClass()) {
	    // it's a member interface in an inner class.
	    throw new SemanticException("Inner classes cannot declare " + 
	            "member interfaces.", n.position());             
	}
	
	// Make sure that static members are not declared inside inner classes
	if (type.isMember() && type.flags().isStatic() 
	        && type.outer().isInnerClass()) {
	    throw new SemanticException("Inner classes cannot declare static " 
	            + "member classes.", n.position());
	}
	
	if (type.superClass() != null) {
	    if (! type.superClass().isClass() || type.superClass().toClass().flags().isInterface()) {
	        throw new SemanticException("Cannot extend non-class \"" +
	                type.superClass() + "\".",
	                n.position());
	    }
	
	    if (type.superClass().toClass().flags().isFinal()) {
	        throw new SemanticException("Cannot extend final class \"" +
	                type.superClass() + "\".",
	                n.position());
	    }
	
	    if (type.typeEquals(ts.Object(), n.context())) {
	        throw new SemanticException("Class \"" + n.classDef() + "\" cannot have a superclass.",
	                n.superClass().position());
	    }
	}
	
	for (Iterator<TypeNode> i = n.interfaces().iterator(); i.hasNext(); ) {
	    TypeNode tn = (TypeNode) i.next();
	    Type t = tn.type();
	
	    if (! t.isClass() || ! t.toClass().flags().isInterface()) {
	        throw new SemanticException("Superinterface " + t + " of " +
	                type + " is not an interface.", tn.position());
	    }
	
	    if (type.typeEquals(ts.Object(), n.context())) {
	        throw new SemanticException("Class " + n.classDef() + " cannot have a superinterface.",
	                tn.position());
	    }
	}
	
	try {
	    if (type.isTopLevel()) {
	        ts.checkTopLevelClassFlags(type.flags());
	    }
	    if (type.isMember()) {
	        ts.checkMemberClassFlags(type.flags());
	    }
	    if (type.isLocal()) {
	        ts.checkLocalClassFlags(type.flags());
	    }
	}
	catch (SemanticException e) {
	    throw new SemanticException(e.getMessage(), n.position());
	}
	
	// Check the class implements all abstract methods that it needs to.
	ts.checkClassConformance(type, n.context());
	
	return n;
    }

    public Node visit(ConstructorDecl_c n) throws SemanticException {
	Context c = n.context();
	
	ClassType ct = c.currentClass();
	
	if (ct.flags().isInterface()) {
	    throw new SemanticException("Cannot declare a constructor inside an interface.",
	                                n.position());
	}
	
	if (ct.isAnonymous()) {
	    throw new SemanticException("Cannot declare a constructor inside an anonymous class.",
	                                n.position());
	}
	
	Name ctName = ct.name();
	
	if (! ctName.equals(n.name().id())) {
	    throw new SemanticException("Constructor name \"" + n.name() +
	                                "\" does not match name of containing class \"" +
	                                ctName + "\".", n.position());
	}
	
	Flags flags = n.flags().flags();
	
	try {
	    ts.checkConstructorFlags(flags);
	}
	catch (SemanticException e) {
	    throw new SemanticException(e.getMessage(), n.position());
	}
	
	if (n.body() == null && ! flags.isNative()) {
	    throw new SemanticException("Missing constructor body.",
	                                n.position());
	}
	
	if (n.body() != null && flags.isNative()) {
	    throw new SemanticException("A native constructor cannot have a body.", n.position());
	}
	
	return n;

    }

    public Node visit(FieldDecl_c n) throws SemanticException {
	
	// Get the fi flags, not the node flags since the fi flags
	// account for being nested within an interface.
	Flags flags = n.fieldDef().flags();
	
	try {
	    ts.checkFieldFlags(flags);
	}
	catch (SemanticException e) {
	    throw new SemanticException(e.getMessage(), n.position());
	}
	
	Type fcontainer = Types.get(n.fieldDef().container());
	
	if (fcontainer.isClass()) {
	    ClassType container = fcontainer.toClass();
	
	    if (container.flags().isInterface()) {
		if (flags.isProtected() || flags.isPrivate()) {
		    throw new SemanticException("Interface members must be public.",
		                                n.position());
		}
	    }
	
	    // check that inner classes do not declare static fields, unless they
	    // are compile-time constants
	    if (flags.isStatic() &&
		    container.isInnerClass()) {
		// it's a static field in an inner class.
		if (!flags.isFinal() || n.init() == null || !n.init().isConstant()) {
		    throw new SemanticException("Inner classes cannot declare " +
		                                "static fields, unless they are compile-time " +
		                                "constant fields.", n.position());
		}
	    }
	}
	
	return n;

    }

    public Node visit(MethodDecl_c n) throws SemanticException {
	// Get the mi flags, not the node flags since the mi flags
	// account for being nested within an interface.
	Flags flags = n.methodDef().flags();
	checkMethodFlags(n, flags);
	overrideMethodCheck(n);
	
	return n;

    }

    private void checkMethodFlags(MethodDecl_c n, Flags flags) throws SemanticException {
	if (n.context().currentClass().flags().isInterface()) {
	    if (flags.isProtected() || flags.isPrivate()) {
	        throw new SemanticException("Interface methods must be public.", n.position());
	    }
	    
	    if (flags.isStatic()) {
		throw new SemanticException("Interface methods cannot be static.", n.position());
	    }
	}
	
	try {
	    ts.checkMethodFlags(flags);
	}
	catch (SemanticException e) {
	    throw new SemanticException(e.getMessage(), n.position());
	}
	
	Type container = Types.get(n.methodDef().container());
	ClassType ct = container.toClass();
	
	if (n.body() == null && ! (flags.isAbstract() || flags.isNative())) {
	    throw new SemanticException("Missing method body.", n.position());
	}
	
	if (n.body() != null && ct.flags().isInterface()) {
	    throw new SemanticException(
		"Interface methods cannot have a body.", n.position());
	}
	
	if (n.body() != null && flags.isAbstract()) {
	    throw new SemanticException(
		"An abstract method cannot have a body.", n.position());
	}
	
	if (n.body() != null && flags.isNative()) {
	    throw new SemanticException(
		"A native method cannot have a body.", n.position());
	}
	
	// check that inner classes do not declare static methods
	if (ct != null && flags.isStatic() && ct.isInnerClass()) {
	    // it's a static method in an inner class.
	    throw new SemanticException("Inner classes cannot declare " + 
	            "static methods.", n.position());             
	}
    }

    private void overrideMethodCheck(MethodDecl_c n) throws SemanticException {
	MethodInstance mi = n.methodDef().asInstance();
	for (Iterator<MethodInstance> j = mi.implemented(n.context()).iterator(); j.hasNext(); ) {
	    MethodInstance mj = (MethodInstance) j.next();
	
	    if (! ts.isAccessible(mj, n.context())) {
	        continue;
	    }
	
	    ts.checkOverride(mi, mj, n.context());
	}
    }

}
