//******************************************************************** 
//* Licensed Materials - Property of IBM 
//* Thorn programming language processor 
//* (C) Copyright IBM Corp. 2009. All Rights Reserved. 
//* US Government Users Restricted Rights - Use, duplication or 
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. 
//******************************************************************** 

package polyglot.interp;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.*;

import polyglot.frontend.Globals;

public class DispatcherMaker {

    public void create(String name, BytecodeCache loader) {
	String className = name;


	Type returnType;
	String methodName;
	Type[] types ;

	if (isSignatureTyped(name)) {
	    System.err.println("Typed guy! " + name);
	    methodName = getMethodNameFromTypedSignature(name);
	    returnType = getReturnType(name);
	    types = getArgTypes(name);
	}
	else {
	    methodName = getMethodNameFromUntypedSignature(name);
	    returnType = Type.getObjectType("java.lang.Object");

	    int arity = getArity(name);
	    types = new Type[arity];
	    for (int i = 0; i < arity; i++) {
		types[i] = Type.getObjectType("java.lang.Object");
	    }
	}

	ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	cw.visit(Opcodes.V1_5, Modifier.INTERFACE | Modifier.PUBLIC, name, null, "java.lang.Object", new String[0]);
	cw.visitSource("<dispatcher for " + methodName + "/" + types.length + ">", null);

	String[] typeDescs = new String[types.length];
	for (int i = 0; i < types.length; i++) {
	    typeDescs[i] = types[i].getDescriptor();
	}
	cw.visitMethod(Modifier.PUBLIC | Modifier.ABSTRACT, className, methodName, returnType.getDescriptor(), typeDescs);
	cw.visitEnd();
	polyglot.interp.BytecodeCache cache = Globals.Extension().bytecodeCache();
	cache.put(null, className, null, cw.toByteArray());
    }

    /**
     * Return true if a new style typed signature ImethodName_ARGTYPES_RETURN
     * and not an old style untyped signature ImethodName_arity
     */
    private boolean isSignatureTyped(String name) {
	int index = name.lastIndexOf('_');
	for (int i = index + 1; i < name.length(); i++) {
	    char ch = name.charAt(i);
	    if ('0' > ch || ch > '9') {
		return true;
	    }
	}
	return false;
    }

    private Type javaType(char desc) {
	switch (desc) {
	case 'A':
	    return Type.getObjectType("java.lang.Object");
	case 'Z':
	    return Type.BOOLEAN_TYPE;
	case 'V':
	    return Type.VOID_TYPE;
	case 'B':
	    return Type.BYTE_TYPE;
	case 'S':
	    return Type.SHORT_TYPE;
	case 'C':
	    return Type.CHAR_TYPE;
	case 'I':
	    return Type.INT_TYPE;
	case 'J':
	    return Type.LONG_TYPE;
	case 'F':
	    return Type.FLOAT_TYPE;
	case 'D':
	    return Type.DOUBLE_TYPE;
	default:
	    return null;
	}
    }

    private Type[] getArgTypes(String name) {
	if (!name.startsWith(prefix)) {
	    return null;
	}

	name = name.substring(prefix.length());

	int last = name.lastIndexOf('_');
	if (last < 0)
	    return null;
	assert last == name.length() - 2;
	int index = name.lastIndexOf('_', last - 1);

	name = name.substring(index + 1, last);

	List<Type> xs = new ArrayList<Type>(name.length());
	for (int i = 0; i < name.length(); i++) {
	    char c = name.charAt(i);
	    xs.add(javaType(c));
	}
	return xs.toArray(new Type[0]);
    }

    private String getMethodNameFromTypedSignature(String name) {
	if (!name.startsWith(prefix)) {
	    return null;
	}

	name = name.substring(prefix.length());

	int last = name.lastIndexOf('_');
	if (last < 0)
	    return null;
	int index = name.lastIndexOf('_', last - 1);

	name = name.substring(0, index);
	return name;
    }

    private Type getReturnType(String name) {
	if (!name.startsWith(prefix)) {
	    return null;
	}

	name = name.substring(prefix.length());

	int index = name.lastIndexOf('_');

	if (index < 0)
	    return null;

	assert index == name.length() - 1;
	return javaType(name.charAt(index + 1));
    }

    static final String prefix = ThornClassLoader.GENERATED_CODE_PACKAGE + ".dispatch.I";

    public String getMethodNameFromUntypedSignature(String name) {
	if (!name.startsWith(prefix)) {
	    return null;
	}

	name = name.substring(prefix.length());

	int index = name.lastIndexOf('_');

	if (index < 0)
	    return null;

	name = name.substring(0, index);
	return name;
    }

    public int getArity(String name) {
	int index = name.lastIndexOf('_');

	if (index < 0)
	    return -1;

	int arity = 0;

	for (int i = index + 1; i < name.length(); i++) {
	    char c = name.charAt(i);
	    if ('0' <= c && c <= '9') {
		arity *= 10;
		arity += c - '0';
	    }
	    else {
		return -1;
	    }
	}

	return arity;
    }
}
