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
import polyglot.util.InternalCompilerError;

public class FrameMaker {
    public void create(final String name, final BytecodeCache loader) {
        final String className = name;
        final List<Type> types = getFieldTypes(name);

	ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
	cw.visit(Opcodes.V1_5, Modifier.PUBLIC, name, null, "java.lang.Object", new String[0]);
	cw.visitSource("<stack frame " + types + ">", null);

	MethodVisitor mv =
	cw.visitMethod(Modifier.PUBLIC, className, "<init>", "V", new String[0]);

	mv.visitCode();
	mv.visitVarInsn(Opcodes.ALOAD, 0);
	mv.visitMethodInsn(Opcodes.INVOKESPECIAL, className, "<init>", Type.VOID_TYPE.getDescriptor());
	mv.visitInsn(Opcodes.RETURN);
	mv.visitEnd();

        int i = 0;
        for (final Type type : types) {
            final String fieldName = "arg" + ++i;
            FieldVisitor fv = cw.visitField(Modifier.PUBLIC, className, types.get(i).getDescriptor(), fieldName, null);
            fv.visitEnd();
        }

	cw.visitEnd();

	polyglot.interp.BytecodeCache cache = Globals.Extension().bytecodeCache();
	cache.put(null, className, null, cw.toByteArray());
    }
    
    public Type parseType(String name, int i) {
	final char c = name.charAt(i);
	switch (c) {
	case 'Z':
	    return (Type.BOOLEAN_TYPE);
	case 'C':
	    return(Type.CHAR_TYPE);
	case 'B':
	    return(Type.BYTE_TYPE);
	case 'S':
	    return(Type.SHORT_TYPE);
	case 'I':
	    return(Type.INT_TYPE);
	case 'J':
	    return(Type.LONG_TYPE);
	case 'F':
	    return(Type.FLOAT_TYPE);
	case 'D':
	    return (Type.DOUBLE_TYPE);
	case 'A':
	    return (Type.getObjectType("java.lang.Object"));
	default:
	    throw new InternalCompilerError("bad type " + name);
	}
    }
    

    public static final String prefix = ThornClassLoader.GENERATED_CODE_PACKAGE + ".Frame$";

    public List<Type> getFieldTypes(String name) {
        if (!name.startsWith(prefix)) {
            return null;
        }

        name = name.substring(prefix.length());

        final List<Type> types = new ArrayList<Type>();

        for (int i = 0; i < name.length(); i++) {
            types.add(parseType(name, i));
        }
        
        return types;
    }
}
