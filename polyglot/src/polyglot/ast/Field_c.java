/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.Flags;
import polyglot.types.Ref;
import polyglot.types.Types;
import polyglot.types.VarInstance;
import polyglot.types.Types.Granularity;
import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.NodeVisitor;

/**
 * A <code>Field</code> is an immutable representation of a Java field
 * access.  It consists of field name and may also have either a 
 * <code>Type</code> or an <code>Expr</code> containing the field being 
 * accessed.
 */
public class Field_c extends Expr_c implements Field
{
    protected Receiver target;
    protected Id name;
    private Ref<FieldInstance> fi;
    protected boolean targetImplicit;

    public Field_c(Position pos, Receiver target, Id name) {
	super(pos);
	assert(target != null && name != null);
	this.target = target;
	this.name = name;
	this.targetImplicit = false;
	this.fi = Types.<FieldInstance>lazyRef(Granularity.LOWER_LEVEL);
    }

    /** Get the target of the field. */
    public Receiver target() {
	return this.target;
    }

    /** Set the target of the field. */
    public Field target(Receiver target) {
	Field_c n = (Field_c) copy();
	n.target = target;
	return n;
    }

    /** Get the name of the field. */
    public Id name() {
	return this.name;
    }

    /** Set the name of the field. */
    public Field name(Id name) {
	Field_c n = (Field_c) copy();
	n.name = name;
	return n;
    }

    /** Return the access flags of the variable. */
    public Flags flags() {
	return fieldInstance().flags();
    }

    /** Get the field instance of the field. */
    public VarInstance varInstance() {
	return fieldInstance();
    }

    /** Get the field instance of the field. */
    public FieldInstance fieldInstance() {
	return Types.get(fi);
    }

    /** Set the field instance of the field. */
    public Field fieldInstance(FieldInstance fi) {
	if (fi == this.fi) return this;
	Field_c n = (Field_c) copy();
	assert fi != null;
	n.fi.update(fi);
	return n;
    }

    public boolean isTargetImplicit() {
	return this.targetImplicit;
    }

    public Field targetImplicit(boolean implicit) {
	Field_c n = (Field_c) copy();
	n.targetImplicit = implicit;
	return n;
    }

    /** Reconstruct the field. */
    protected Field_c reconstruct(Receiver target, Id name) {
	if (target != this.target || name != this.name) {
	    Field_c n = (Field_c) copy();
	    n.target = target;
	    n.name = name;
	    return n;
	}

	return this;
    }

    /** Visit the children of the field. */
    public Node visitChildren(NodeVisitor v) {
	Receiver target = (Receiver) visitChild(this.target, v);
	Id name = (Id) visitChild(this.name, v);
	return reconstruct(target, name);
    }

    public void dump(CodeWriter w) {
	super.dump(w);

	if (fi != null) {                       
	    w.allowBreak(4, " ");
	    w.begin(0);
	    w.write("(instance " + fi + ")");   
	    w.end();                            
	}                                       

	w.allowBreak(4, " ");
	w.begin(0);
	w.write("(name \"" + name + "\")");
	w.end();
    }

    public String toString() {
	return (target != null ? target + "." : "") + name;
    }

    /**
     * Check the consistency of the implicit target inserted by the compiler by
     * asserting that the FieldInstance in the Context for this field's name is
     * the same as the FieldInstance we assigned to this field.
     */
    public void checkConsistency(Context c) {
	if (targetImplicit) {
	    VarInstance vi = c.findVariableSilent(name.id());
	    if (vi instanceof FieldInstance) {
		FieldInstance rfi = (FieldInstance) vi;
		// Compare the original (declaration) fis, not the actuals.
		// We do this because some extensions that do type substitutions
		// perform the substitution
		// on the fi after lookup and some extensions modify lookup
		// itself to do the substitution.
		FieldInstance fi = fieldInstance();
		if (c.typeSystem().equals(rfi.def(), fi.def())) {
		    // all is OK.
		    return;
		}
//		System.out.println("(found) rfi is " + rfi.def());
//		System.out.println("(actual) fi is " + fi.def());
	    }
	    throw new InternalCompilerError("Field " + this + " has an " +
	                                    "implicit target, but the name " + name.id() + " resolves to " +
	                                    vi + " instead of " + target, position());
	}      
    }

    public Ref<FieldInstance> fieldInstanceRef() {
	return fi;
    }


}
