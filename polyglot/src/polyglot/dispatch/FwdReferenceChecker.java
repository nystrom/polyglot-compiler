/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.dispatch;

import java.util.HashSet;
import java.util.Set;

import polyglot.ast.*;
import polyglot.frontend.Globals;
import polyglot.frontend.Job;
import polyglot.types.*;
import polyglot.util.ErrorInfo;

/**
 * Visitor which ensures that field intializers and initializers do not make
 * illegal forward references to fields. This is an implementation of the rules
 * of the Java Language Spec, 2nd Edition, Section 8.3.2.3
 */
public class FwdReferenceChecker extends Visitor {
    public FwdReferenceChecker(Job job, TypeSystem ts, NodeFactory nf) {}

    public static class Ctx {
	boolean inInitialization = false;
	boolean inStaticInit = false;
	Set<FieldDef> declaredFields = new HashSet<FieldDef>();

	Ctx copy() {
	    Ctx c = new Ctx();
	    c.inInitialization = inInitialization;
	    c.inStaticInit = inStaticInit;
	    c.declaredFields = new HashSet<FieldDef>(declaredFields);
	    return c;
	}
    }
    
    public Node visit(SourceFile n, Ctx ctx) {
	return acceptChildren(n, new Ctx());
    }
    
    public Node visit(ClassBody n, Ctx ctx) {
	return acceptChildren(n, new Ctx());
    }
    
    public Node visit(Node n, Ctx ctx) {
	return acceptChildren(n, ctx);
    }

    public Node visit(FieldDecl fd, Ctx ctx) {
	// Add the field to the set of declared fields.
	ctx.declaredFields.add(fd.fieldDef());

	Ctx c = ctx.copy();
	c.inInitialization = true;
	c.inStaticInit = fd.flags().flags().isStatic();
	return acceptChildren(fd, c);
    }

    public Node visit(Initializer n, Ctx ctx) {
	Ctx c = ctx.copy();
	c.inInitialization = true;
	c.inStaticInit = n.flags().flags().isStatic();
	return acceptChildren(n, c);
    }

    public Node visit(Assign n, Ctx ctx) {
	if (n.left() instanceof Field) {
	    Field f = (Field) n.left();
	    accept(f.target(), ctx);
	    accept(n.right(), ctx);
	    return n;
	}
	
	return acceptChildren(n, ctx);
    }

    public Node visit(Field f, Ctx ctx) {
	if (ctx.inInitialization) {
	    // we need to check if this is an illegal fwd reference.

	    // an illegal fwd reference if a usage of an instance
	    // (resp. static) field occurs in an instance (resp. static)
	    // initialization, and the innermost enclosing class or
	    // interface of the usage is the same as the container of
	    // the field, and we have not yet seen the field declaration.
	    //
	    // In addition, if a field is not accessed as a simple name,
	    // then all is ok

	    ClassDef currentClass = f.context().currentClassDef();
	    Type fContainer = f.fieldInstance().container();

	    if (ctx.inStaticInit == f.fieldInstance().flags().isStatic() &&
		    currentClass == fContainer.toClass().def() &&
		    !ctx.declaredFields.contains(f.fieldInstance().def()) &&
		    f.isTargetImplicit()) {

		Globals.Compiler().errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR, "Illegal forward reference.", f.position());
	    }
	}
	
	return acceptChildren(f, ctx);
    }
}
