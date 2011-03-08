/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2007 IBM Corporation
 * 
 */

package polyglot.visit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.*;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.*;

/** Visitor which traverses the AST constructing type objects. */
public class TypeBuilderContext implements Copy
{
    protected ImportTable importTable;
    protected Job job;
    protected TypeSystem ts;
    protected NodeFactory nf;
    protected TypeBuilderContext outer;
    protected boolean inCode; // true if the last scope pushed as not a class.
    protected boolean global; // true if all scopes pushed have been classes.
    protected Package package_;
    protected ClassDef type; // last class pushed.
    protected Def def;
    protected boolean inStaticContext;
    
    public TypeBuilderContext(Job job, TypeSystem ts, NodeFactory nf) {
        this.job = job;
        this.ts = ts;
        this.nf = nf;
        this.outer = null;
        this.inStaticContext = false; //true;
    }
    
    public Object copy() {
	try {
	    return super.clone();
	}
	catch (CloneNotSupportedException e) {
	    throw new InternalCompilerError(e);
	}
    }
    
    public TypeBuilderContext push() {
        TypeBuilderContext tb = (TypeBuilderContext) this.copy();
        tb.outer = this;
        return tb;
    }
    
    public boolean inCode() {
	return inCode;
    }

    public TypeBuilderContext pop() {
        return outer;
    }
    
    public Def def() {
        return def;
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
    

    /**
    @deprecated */
    public TypeBuilderContext pushContext(Context c) throws SemanticException {
        LinkedList<Context> stack = new LinkedList<Context>();
        while (c != null) {
            stack.addFirst(c);
            c = c.pop();
        }
        
        TypeBuilderContext tb = this;
        boolean inCode = false;
        for (Iterator<Context> i = stack.iterator(); i.hasNext(); ) {
            c = (Context) i.next();
            if (c.inCode()) {
                if (! inCode) {
                    // entering code
                    inCode = true;
                    tb = tb.pushCode(c.currentCode());
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
                if (c.currentClassDef() != tb.currentClass()) {
                    // entering class
                    tb = tb.pushClass(c.currentClassDef());
                }
            }
        }
        
        return tb;
    }
    

    public TypeBuilderContext pushStatic(boolean flag) {
	if (flag == inStaticContext)
	    return this;
	TypeBuilderContext tb = push();
	tb.inStaticContext = flag;
	return tb;
    }

    public TypeBuilderContext pushDef(Def def) {
        TypeBuilderContext tb = push();
        tb.def = def;
        return tb;
    }
    
    public TypeBuilderContext pushPackage(Package p) {
        if (Report.should_report(Report.visit, 4))
	    Report.report(4, "TB pushing package " + p + ": " + context());
        TypeBuilderContext tb = push();
        tb.inCode = false;
        tb.package_ = p;
        return tb;
    }

    public TypeBuilderContext pushCode(CodeDef def) {
        if (Report.should_report(Report.visit, 4))
	    Report.report(4, "TB pushing code: " + context());
        TypeBuilderContext tb = pushDef(def);
        tb.inCode = true;
        tb.global = false;
        return tb;
    }

    public TypeBuilderContext pushClass(ClassDef classDef) throws SemanticException {
        if (Report.should_report(Report.visit, 4))
	    Report.report(4, "TB pushing class " + classDef + ": " + context());

        TypeBuilderContext tb = pushDef(classDef);
        tb.inCode = false;
        tb.type = classDef;

	// Make sure the import table finds this class.
        if (importTable() != null && classDef.isTopLevel()) {
	    tb.importTable().addExplicitImport(QName.make(classDef.fullName()));
	}
        
        return tb;
    }

    protected ClassDef newClass(Position pos, Flags flags, Name name)
        throws SemanticException
    {
	TypeSystem ts = typeSystem();

        ClassDef ct = ts.createClassDef(job().source());

        ct.position(pos);
        ct.flags(flags);
        ct.name(name);
        ct.superType(new ErrorRef_c<Type>(ts, pos, "Cannot get superclass before type-checking class declaration."));

	if (inCode) {
            ct.kind(ClassDef.LOCAL);
	    ct.outer(Types.ref(currentClass()));
	    ct.setJob(job());

	    if (currentPackage() != null) {
	      	ct.setPackage(Types.<Package>ref(currentPackage()));
	    }

	    return ct;
	}
	else if (currentClass() != null) {
            ct.kind(ClassDef.MEMBER);
            ct.outer(Types.ref(currentClass()));
	    ct.setJob(job());

	    currentClass().addMemberClass(Types.<ClassType>ref(ct.asType()));

	    if (currentPackage() != null) {
	      	ct.setPackage(Types.<Package>ref(currentPackage()));
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
                typeSystem().systemResolver().addNamed(QName.make(currentClass().fullName(), ct.name()), ct.asType());

                // Save in the cache using the name a class file would use.
                QName classFileName = typeSystem().getTransformedClassName(ct);
                typeSystem().systemResolver().install(classFileName, ct.asType());
            }

            return ct;
	}
	else {
            ct.kind(ClassDef.TOP_LEVEL);
            ct.setJob(job());

            QName fullName;

            if (currentPackage() != null) {
        	ct.setPackage(Types.<Package>ref(currentPackage()));
        	fullName = QName.make(currentPackage().fullName(), ct.name());
            }
            else {
        	fullName = QName.make(null, ct.name());
            }

            Named dup = typeSystem().systemResolver().check(fullName);

            if (dup != null && dup.fullName().equals(fullName)) {
                throw new SemanticException("Duplicate class \"" +
                                            ct.fullName() + "\".", pos);
            }

            typeSystem().systemResolver().addNamed(fullName, ct.asType());

	    return ct;
	}
    }

    public TypeBuilderContext pushAnonClass(Position pos) throws SemanticException {
        if (Report.should_report(Report.visit, 4))
	    Report.report(4, "TB pushing anon class: " + this);

        if (! inCode) {
            throw new InternalCompilerError(
                "Can only push an anonymous class within code.");
        }

	TypeSystem ts = typeSystem();

        ClassDef ct = ts.createClassDef(this.job().source());
        ct.kind(ClassDef.ANONYMOUS);
        ct.outer(Types.ref(currentClass()));
        ct.position(pos);
        ct.setJob(job());

        if (currentPackage() != null) {
            ct.setPackage(Types.<Package>ref(currentPackage()));
        }
        
//        ct.superType(ts.unknownType(pos));

        return pushClass(ct);
    }

    public TypeBuilderContext pushClass(Position pos, Flags flags, Name name)
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

    public boolean inStaticContext() {
	return inStaticContext;
    }
}
