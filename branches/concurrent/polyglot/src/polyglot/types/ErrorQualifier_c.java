/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;


/**
 * An unknown type qualifier.  This is used as a place-holder until types
 * are disambiguated.
 */
public class ErrorQualifier_c extends TypeObject_c implements ErrorQualifier
{
    public ErrorQualifier_c(TypeSystem ts) {
        super(ts);
    }

    public boolean isPackage() { return false; }
    public boolean isType() { return false; }

    public Package toPackage() { return null; }
    public Type toType() { return null; }

    public String toString() {
        return "<error>";
    }
}
