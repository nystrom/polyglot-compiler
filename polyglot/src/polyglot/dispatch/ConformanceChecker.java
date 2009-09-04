package polyglot.dispatch;

import java.util.Iterator;

import polyglot.ast.*;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.visit.ContextVisitor;

public class ConformanceChecker {
    Job job;
    NodeFactory nf;
    TypeSystem ts;

    public Node visit(Node n) throws SemanticException {
	return n.acceptChildren(this);
    }

    ContextVisitor cc(Node n) throws SemanticException {
	ContextVisitor cc = new ContextVisitor(job, ts, nf);
	cc = cc.context(n.context());
	return cc;
    }


    Node visit(ClassBody n) throws SemanticException {
	ContextVisitor cc = cc(n);
	ClassBody_c r = (ClassBody_c) n;
	r.duplicateFieldCheck(cc);
	r.duplicateConstructorCheck(cc);
	r.duplicateMethodCheck(cc);
	r.duplicateMemberClassCheck(cc);
	return r;

    }

    Node visit(ClassDecl n) throws SemanticException {
	ContextVisitor cc = cc(n);
	ClassDecl_c r = (ClassDecl_c) n;
	
	ClassType type = r.classDef().asType();
	Name name = r.name().id();
	
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
	                        "same name.", r.position());
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
	    Context ctxt = cc.context();

	    if (ctxt.isLocal(name)) {
	        // Something with the same name was declared locally.
	        // (but not in an enclosing class)                                    
	        Named nm = ctxt.find(ts.TypeMatcher(name));
	        if (nm instanceof Type) {
	            Type another = (Type) nm;
	            if (another.isClass() && another.toClass().isLocal()) {
	                throw new SemanticException("Cannot declare local " +
	                        "class \"" + r.classDef().name() + "\" within the same " +
	                        "method, constructor or initializer as another " +
	                        "local class of the same name.", r.position());
	            }
	        }
	    }                
	}
	
	// check that inner classes do not declare member interfaces
	if (type.isMember() && type.flags().isInterface() &&
	        type.outer().isInnerClass()) {
	    // it's a member interface in an inner class.
	    throw new SemanticException("Inner classes cannot declare " + 
	            "member interfaces.", r.position());             
	}
	
	// Make sure that static members are not declared inside inner classes
	if (type.isMember() && type.flags().isStatic() 
	        && type.outer().isInnerClass()) {
	    throw new SemanticException("Inner classes cannot declare static " 
	            + "member classes.", r.position());
	}
	
	if (type.superClass() != null) {
	    if (! type.superClass().isClass() || type.superClass().toClass().flags().isInterface()) {
	        throw new SemanticException("Cannot extend non-class \"" +
	                type.superClass() + "\".",
	                r.position());
	    }
	
	    if (type.superClass().toClass().flags().isFinal()) {
	        throw new SemanticException("Cannot extend final class \"" +
	                type.superClass() + "\".",
	                r.position());
	    }
	
	    if (type.typeEquals(ts.Object(), cc.context())) {
	        throw new SemanticException("Class \"" + r.classDef() + "\" cannot have a superclass.",
	                r.superClass().position());
	    }
	}
	
	for (Iterator<TypeNode> i = r.interfaces().iterator(); i.hasNext(); ) {
	    TypeNode tn = (TypeNode) i.next();
	    Type t = tn.type();
	
	    if (! t.isClass() || ! t.toClass().flags().isInterface()) {
	        throw new SemanticException("Superinterface " + t + " of " +
	                type + " is not an interface.", tn.position());
	    }
	
	    if (type.typeEquals(ts.Object(), cc.context())) {
	        throw new SemanticException("Class " + r.classDef() + " cannot have a superinterface.",
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
	    throw new SemanticException(e.getMessage(), r.position());
	}
	
	// Check the class implements all abstract methods that it needs to.
	ts.checkClassConformance(type, cc.context());
	
	return r;
    }

    Node visit(ConstructorDecl n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }

    Node visit(FieldDecl n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }

    Node visit(Formal n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }

    Node visit(Initializer n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }

    Node visit(Local n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }

    Node visit(LocalDecl n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }

    Node visit(MethodDecl n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }

    Node visit(New n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }

    Node visit(Return n) throws SemanticException {
	ContextVisitor cc = cc(n);
	return n.del().conformanceCheck(cc);

    }
}
