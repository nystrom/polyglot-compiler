/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.types;

import polyglot.util.InternalCompilerError;
import polyglot.util.StringUtil;

/**
 * A <code>PackageContextResolver</code> is responsible for looking up types
 * and packages in a package by name.
 */
public class PackageContextResolver extends AbstractAccessControlResolver
{
    protected Package p;

    /**
     * Create a package context resolver.
     * @param ts The type system.
     * @param p The package in whose context to search.
     */
    public PackageContextResolver(TypeSystem ts, Package p) {
        super(ts);
	this.p = p;
    }

    /**
     * The package in whose context to search.
     */
    public Package package_() {
        return p;
    }

    /**
     * Find a type object by name.
     * @param name Name of the class or package to find.
     * @param accessor
     *                Class the name is accesses from. If null, no access checks
     *                are performed.
     * 
     */
    public Named find(Matcher<Named> matcher, ClassDef accessor) throws SemanticException {
	Name name = matcher.name();
	
        Named n = null;

	try {
	    n = ts.systemResolver().find(QName.make(p.fullName(), name));
	}
	catch (NoClassException e) {
            // Rethrow if some _other_ class or package was not found.
            if (!e.getClassName().equals(p.fullName() + "." + name)) {
                throw e;
            }
	}

        if (n == null) {
            n = ts.createPackage(p, name);
        }
        
        if (! canAccess(n, accessor)) {
            throw new SemanticException("Cannot access " + n + " from " + accessor + ".");
        }
        
        return n;
    }

    protected boolean canAccess(Named n, ClassDef accessor) {
        if (n instanceof ClassType) {
            return accessor == null || ts.classAccessible(((ClassType) n).def(), accessor);
        }
        return true;
    }

    public String toString() {
        return "(package-context " + p.toString() + ")";
    }
}
