/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.util.CodeWriter;
import polyglot.util.InternalCompilerError;

/**
 * An unknown type.  This is used as a place-holder until types are
 * disambiguated.
 */
public class ErrorPackage_c extends Package_c implements ErrorPackage
{
    /** Used for deserializing types. */
    protected ErrorPackage_c() { }
    
    /** Creates a new type in the given a TypeSystem. */
    public ErrorPackage_c(TypeSystem ts) {
        super(ts);
    }

    public String translate(Resolver c) {
	throw new InternalCompilerError("Cannot translate an error package.");
    }

    public String toString() {
	return "<error>";
    }
    public void print(CodeWriter w) {
	w.write(toString());
    }
}
