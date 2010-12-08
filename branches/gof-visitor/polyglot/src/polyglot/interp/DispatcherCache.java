//******************************************************************** 
//* Licensed Materials - Property of IBM 
//* Thorn programming language processor 
//* (C) Copyright IBM Corp. 2009. All Rights Reserved. 
//* US Government Users Restricted Rights - Use, duplication or 
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. 
//******************************************************************** 

package polyglot.interp;

import java.util.*;

import polyglot.types.Name;

public class DispatcherCache {

    public DispatcherCache() {
        cache = new HashMap<String,Class<?>>();
    }

    Map<String,Class<?>> cache;

    char descOfJavaType(final   polyglot.bytecode.types.Type t) {
	char ch = t.desc().charAt(0);
    if (ch == 'L' || ch == '[') ch = 'A';
    return ch;
    }

    public String dispatcherName(Name methodName, final List<polyglot.bytecode.types.Type> argTypes, final polyglot.bytecode.types.Type returnType) {
        methodName = methodName.mangle();

        boolean useUntypedSignature = true;
        final StringBuilder sig = new StringBuilder();

        for (final polyglot.bytecode.types.Type t : argTypes) {
        	final char desc = descOfJavaType(t);
        	sig.append(desc);
        	if (desc != 'A') {
        		useUntypedSignature = false;
        	}
        }

        final char returnTypeDesc = descOfJavaType(returnType);
        if (returnTypeDesc != 'A') {
        	useUntypedSignature = false;
        }

        String iname;

        if (useUntypedSignature) {
        	final int args = argTypes.size();

        	if (methodName == Name.make("==").mangle() && args == 1)
        		return "thorn.runtime.common.IObject";

        	iname = "I" + methodName + "_" + args;
        }
        else {
        	iname = "I" + methodName + "_" + returnTypeDesc + "_" + sig.toString();
        }

        Class<?> c = cache.get(iname);

        if (c != null) {
            return c.getName();
        }

        // Then, try the pre-built interfaces.
        try {
            final String dispatchClassName = "thorn.runtime.compiler.dispatch." + iname;
            c = Class.forName(dispatchClassName);
            cache.put(iname, c);
            return c.getName();
        }
        catch (final ClassNotFoundException e) {
        }
        catch (final NoClassDefFoundError e) {
            // FIXME: shouldn't catch an error--should have our own ClassLoader, maybe.
            // This can happen if the class file does not contain the expected
            // class, i.e., if we're looking for IString_0 and load
            // Istring_0.class instead because of case insensitivity in the file
            // system.
        }

        try {
            c = Class.forName("thorn.ast.generated." + iname);
            cache.put(iname, c);
            return c.getName();
        }
        catch (final ClassNotFoundException e) {
        }
        catch (final NoClassDefFoundError e) {
        }

        final String fullName = "polyglot.generated.dispatch." + iname;
        return fullName;
    }

    public String dispatcherName(Name methodName, final int args) {
	methodName = methodName.mangle();
	
        if (methodName == Name.make("==").mangle() && args == 1)
            return "thorn.runtime.common.IObject";

        final String iname = "I" + methodName + "_" + args;

        Class<?> c = cache.get(iname);

        if (c != null) {
            return c.getName();
        }

        // Then, try the pre-built interfaces.
        try {
            final String dispatchClassName = "thorn.runtime.compiler.dispatch." + iname;
            c = Class.forName(dispatchClassName);
            cache.put(iname, c);
            return c.getName();
        }
        catch (final ClassNotFoundException e) {
        }
        catch (final NoClassDefFoundError e) {
            // FIXME: shouldn't catch an error--should have our own ClassLoader, maybe.
            // This can happen if the class file does not contain the expected
            // class, i.e., if we're looking for IString_0 and load
            // Istring_0.class instead because of case insensitivity in the file
            // system.
        }

        try {
            c = Class.forName("polyglot.generated." + iname);
            cache.put(iname, c);
            return c.getName();
        }
        catch (final ClassNotFoundException e) {
        }
        catch (final NoClassDefFoundError e) {
        }

        final String fullName = "polyglot.generated.dispatch." + iname;
        return fullName;
    }
}
