/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.*;
import polyglot.types.Package;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.Translator;

/**
 * A <code>PackageNode</code> is the syntactic representation of a 
 * Java package within the abstract syntax tree.
 */
public class PackageNode_c extends Node_c implements PackageNode
{
    protected Ref<Package> package_;

    public PackageNode_c(Position pos, Ref<Package> package_) {
	super(pos);
	assert(package_ != null);
	this.package_ = package_;
    }
    
    /** Get the package as a qualifier. */
    public Ref<Qualifier> qualifierRef() {
        return (Ref<Qualifier>) (Ref) this.package_;
    }

    /** Get the package. */
    public Ref<Package> package_() {
	return this.package_;
    }

    /** Set the package. */
    public PackageNode package_(Ref<Package> package_) {
	PackageNode_c n = (PackageNode_c) copy();
	n.package_ = package_;
	return n;
    }
    
    public void translate(CodeWriter w, Translator tr) {
        w.write(package_.get().translate(tr.context()));
    }

    public String toString() {
        return package_.toString();
    }
    

}
