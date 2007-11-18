/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2007 IBM Corporation
 * 
 */

package polyglot.visit;

import java.util.Iterator;
import java.util.LinkedList;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.*;

/** Visitor which traverses the AST constructing type objects. */
public class TypeBuilder extends NodeVisitor
{
    protected ImportTable importTable;
    protected Job job;
    protected TypeSystem ts;
    protected NodeFactory nf;
    protected TypeBuilder outer;
    protected boolean inCode; // true if the last scope pushed as not a class.
    protected boolean global; // true if all scopes pushed have been classes.
    protected Package package_;
    protected ClassDef type; // last class pushed.

    public TypeBuilder(Job job, TypeSystem ts, NodeFactory nf) {
        this.job = job;
        this.ts = ts;
        this.nf = nf;
        this.outer = null;
    }
    
    public TypeBuilder push() {
        TypeBuilder tb = (TypeBuilder) this.copy();
        tb.outer = this;
        return tb;
    }

    public TypeBuilder pop() {
        return outer;
    }
    
    public Job job() {
        return job;
    }
    
    public ErrorQueue errorQueue() {
        return job().compiler().errorQueue();
    }

    public NodeFactory nodeFactory() {
        return nf;
    }

    public TypeSystem typeSystem() {
        return ts;
    }

    public NodeVisitor begin() {
        return this;
    }

    public NodeVisitor enter(Node n) {
        try {
	    return n.del().buildTypesEnter(this);
	}
	catch (SemanticException e) {
	    Position position = e.position();

	    if (position == null) {
		position = n.position();
	    }

            if (e.getMessage() != null) {
                errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                                    e.getMessage(), position);
            }
                            
            return this;
	}
    }

    public Node leave(Node old, Node n, NodeVisitor v) {
	try {
	    return n.del().buildTypes((TypeBuilder) v);
	}
	catch (SemanticException e) {
	    Position position = e.position();

	    if (position == null) {
		position = n.position();
	    }

            if (e.getMessage() != null) {
                errorQueue().enqueue(ErrorInfo.SEMANTIC_ERROR,
                                    e.getMessage(), position);
            }

	    return n;
	}
    }

    public TypeBuilder pushContext(Context c) throws SemanticException {
        LinkedList stack = new LinkedList();
        while (c != null) {
            stack.addFirst(c);
            c = c.pop();
        }
        
        TypeBuilder tb = this;
        boolean inCode = false;
        for (Iterator i = stack.iterator(); i.hasNext(); ) {
            c = (Context) i.next();
            if (c.inCode()) {
                if (! inCode) {
                    // entering code
                    inCode = true;
                    tb = tb.pushCode();
                }
            }
            else {
                if (c.importTable() != null && tb.importTable() == null) {
                    // entering class file
                    tb.setImportTable(c.importTable());
                }
                if (c.importTable() != null && c.package_() != null &&
                    tb.currentPackage() == null) {
                    // entering package context in source
                    tb = tb.pushPackage(c.package_());
                }
                if (c.currentClassScope() != tb.currentClass()) {
                    // entering class
                    tb = tb.pushClass(c.currentClassScope());
                }
            }
        }
        
        return tb;
    }
        
    public TypeBuilder pushPackage(Package p) {
        if (Report.should_report(Report.visit, 4))
	    Report.report(4, "TB pushing package " + p + ": " + context());
        TypeBuilder tb = push();
        tb.inCode = false;
        tb.package_ = p;
        return tb;
    }

    public TypeBuilder pushCode() {
        if (Report.should_report(Report.visit, 4))
	    Report.report(4, "TB pushing code: " + context());
        TypeBuilder tb = push();
        tb.inCode = true;
        tb.global = false;
        return tb;
    }

    protected TypeBuilder pushClass(ClassDef classDef) throws SemanticException {
        if (Report.should_report(Report.visit, 4))
	    Report.report(4, "TB pushing class " + classDef + ": " + context());

        TypeBuilder tb = push();
        tb.type = classDef;
        tb.inCode = false;

	// Make sure the import table finds this class.
        if (importTable() != null && classDef.isTopLevel()) {
	    tb.importTable().addClassImport(classDef.fullName());
	}
        
        return tb;
    }

    protected ClassDef newClass(Position pos, Flags flags, String name)
        throws SemanticException
    {
	TypeSystem ts = typeSystem();

        ClassDef ct = ts.createClassType(job().source());
        LazyClassInitializer init = (LazyClassInitializer) ct.initializer();

        ct.position(pos);
        ct.flags(flags);
        ct.name(name);
        ct.superType(new ErrorRef_c<Type>(ts, pos));

	if (inCode) {
            ct.kind(ClassDef.LOCAL);
	    ct.outer(Ref_c.ref(currentClass()));
	    ct.setJob(job());

	    if (currentPackage() != null) {
	      	ct.package_(Ref_c.<Package>ref(currentPackage()));
	    }

	    return ct;
	}
	else if (currentClass() != null) {
            ct.kind(ClassDef.MEMBER);
            ct.outer(Ref_c.ref(currentClass()));
	    ct.setJob(job());

	    currentClass().addMemberClass(Ref_c.<ClassType>ref(ct.asType()));

	    if (currentPackage() != null) {
	      	ct.package_(Ref_c.<Package>ref(currentPackage()));
	    }

            // if all the containing classes for this class are member
            // classes or top level classes, then add this class to the
            // parsed resolver.
            ClassDef container = currentClass();
            boolean allMembers = (container.isMember() || container.isTopLevel());
            while (container.isMember()) {
                container = container.outer().get();
                allMembers = allMembers && 
                (container.isMember() || container.isTopLevel());
            }

            if (allMembers) {
                typeSystem().systemResolver().addNamed(ct.fullName(), ct);

                // Save in the cache using the name a class file would use.
                String classFileName = typeSystem().getTransformedClassName(ct);
                typeSystem().systemResolver().install(classFileName, ct);
            }

            return ct;
	}
	else {
            ct.kind(ClassDef.TOP_LEVEL);
            ct.setJob(job());

	    if (currentPackage() != null) {
	      	ct.package_(Ref_c.<Package>ref(currentPackage()));
	    }

            Named dup = typeSystem().systemResolver().check(ct.fullName());

            if (dup != null && dup.fullName().equals(ct.fullName())) {
                throw new SemanticException("Duplicate class \"" +
                                            ct.fullName() + "\".", pos);
            }

            typeSystem().systemResolver().addNamed(ct.fullName(), ct);

	    return ct;
	}
    }

    public TypeBuilder pushAnonClass(Position pos) throws SemanticException {
        if (Report.should_report(Report.visit, 4))
	    Report.report(4, "TB pushing anon class: " + this);

        if (! inCode) {
            throw new InternalCompilerError(
                "Can only push an anonymous class within code.");
        }

	TypeSystem ts = typeSystem();

        ClassDef ct = ts.createClassType(this.job().source());
        ct.kind(ClassDef.ANONYMOUS);
        ct.outer(Ref_c.ref(currentClass()));
        ct.position(pos);
        ct.setJob(job());

        if (currentPackage() != null) {
            ct.package_(Ref_c.<Package>ref(currentPackage()));
        }
        
//        ct.superType(ts.unknownType(pos));

        return pushClass(ct);
    }

    public TypeBuilder pushClass(Position pos, Flags flags, String name)
    	throws SemanticException {

        ClassDef t = newClass(pos, flags, name);
        return pushClass(t);
    }

    public ClassDef currentClass() {
        return this.type;
    }

    public Package currentPackage() {
        return package_;
    }

    public ImportTable importTable() {
        return importTable;
    }

    public void setImportTable(ImportTable it) {
        this.importTable = it;
    }

    public String context() {
        return "(TB " + type +
                (inCode ? " inCode" : "") +
                (global ? " global" : "") +
                (outer == null ? ")" : " " + outer.context() + ")");
    }
}
