/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import polyglot.types.QName;
import polyglot.util.Position;

/**
 * An <code>Import</code> is an immutable representation of a Java
 * <code>import</code> statement.  It consists of the string representing the
 * item being imported and the kind which is either indicating that a class
 * is being imported, or that an entire package is being imported.
 */
public class Import_c extends Node_c implements Import
{
    protected Kind kind;
    protected QName name;

    public Import_c(Position pos, Kind kind, QName name) {
	super(pos);
	assert(kind != null && name != null);
	this.name = name;
	this.kind = kind;
    }

    /** Get the name of the import. */
    public QName name() {
	return this.name;
    }

    /** Set the name of the import. */
    public Import name(QName name) {
	Import_c n = (Import_c) copy();
	n.name = name;
	return n;
    }

    /** Get the kind of the import. */
    public Kind kind() {
	return this.kind;
    }

    /** Set the kind of the import. */
    public Import kind(Kind kind) {
	Import_c n = (Import_c) copy();
	n.kind = kind;
	return n;
    }

    /**
     * Build type objects for the import.
    public Node buildTypes(TypeBuilder tb) throws SemanticException {
	ImportTable it = tb.importTable();

	if (kind == CLASS) {
	    it.addClassImport(name);
	}
	else if (kind == PACKAGE) {
	    it.addPackageImport(name);
	}

	return this;
    }
     */

    public String toString() {
	return "import " + name + (kind == PACKAGE ? ".*" : "");
    }


}
