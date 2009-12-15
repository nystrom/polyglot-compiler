/**
 * 
 */
package polyglot.bytecode.types;

import polyglot.types.ClassDef;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;

public class Type {

    private Type(String d) {
        assert !d.equals("L");
        desc = d;
    }
    
    @Override
    public int hashCode() {
        return desc.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        return o instanceof Type && ((Type) o).desc.equals(desc);
    }

    public String className() {
        if (isObject())
            return desc.substring(1, desc.length() - 1);
        if (isArray())
            return desc;
        throw new InternalCompilerError("Cannot get class name of type " + desc);
    }

    public String desc() {
        return desc;
    }

    String getDescriptor() {
        return desc();
    }

    String desc;

    public boolean isAddress() {
        return desc.equals("A");
    }

    public boolean isUninitialized() {
        return desc.equals("U");
    }

    public boolean isVoid() {
        return desc.equals("V");
    }

    public boolean isBoolean() {
        return desc.equals("Z");
    }

    public boolean isByte() {
        return desc.equals("B");
    }

    public boolean isChar() {
        return desc.equals("C");
    }

    public boolean isShort() {
        return desc.equals("S");
    }

    public boolean isInt() {
        return desc.equals("I");
    }

    public boolean isLong() {
        return desc.equals("J");
    }

    public boolean isFloat() {
        return desc.equals("F");
    }

    public boolean isDouble() {
        return desc.equals("D");
    }

    public boolean isArray() {
        return desc.startsWith("[");
    }

    public Type arrayBase() {
        assert isArray();
        return new Type(desc.substring(1));
    }

    public boolean isObject() {
        return desc.startsWith("L");
    }

    public boolean isRef() {
        return isObject() || isArray() || isAddress() || isUninitialized();
    }

    public boolean isWide() {
        return isLong() || isDouble();
    }

    public boolean isNarrow() {
        return !isWide();
    }

    public boolean isIType() {
        return isBoolean() || isByte() || isShort() || isChar() || isInt();
    }

    public static final Type NULL = new Type("L;");
    public static final Type OBJECT = new Type("Ljava/lang/Object;");
    public static final Type STRING = new Type("Ljava/lang/String;");
    public static final Type CLASS = new Type("Ljava/lang/Class;");
    public static final Type SB = new Type("Ljava/lang/StringBuilder;");
    public static final Type VOID = new Type("V");
    public static final Type BOOLEAN = new Type("Z");
    public static final Type BYTE = new Type("B");
    public static final Type CHAR = new Type("C");
    public static final Type SHORT = new Type("S");
    public static final Type INT = new Type("I");
    public static final Type LONG = new Type("J");
    public static final Type FLOAT = new Type("F");
    public static final Type DOUBLE = new Type("D");

    public static Type typeFromPolyglotType(polyglot.types.Type t) {
        if (t.isArray())
            return array(typeFromPolyglotType(t.toArray().base()));
        if (t.isBoolean())
            return Type.BOOLEAN;
        if (t.isByte())
            return Type.BYTE;
        if (t.isShort())
            return Type.SHORT;
        if (t.isChar())
            return Type.CHAR;
        if (t.isInt())
            return Type.INT;
        if (t.isLong())
            return Type.LONG;
        if (t.isFloat())
            return Type.FLOAT;
        if (t.isDouble())
            return Type.DOUBLE;
        if (t.isVoid())
            return Type.VOID;
        if (t.isClass()) {
            if (t.toClass().isTopLevel()) {
                return typeFromClassName(t.toClass().fullName().toString());
            }
            else if (t.toClass().isMember()) {
                ClassDef def = t.toClass().def();
                polyglot.types.Type outer = Types.get(def.container());
                return typeFromClassName(outer.toClass().fullName().toString() + "$" + def.name().toString());
            }
            else {
                throw new InternalCompilerError("Not TopLevel or Member.");
            }
        }
        if (t.isNull())
            return Type.NULL;
        throw new InternalCompilerError("Unknown type " + t);
    }

    public static Type typeFromDescriptor(String desc) {
        return new Type(desc);
    }

    public static Type typeFromClassName(String desc) {
        return new Type("L" + desc.replace('.', '/') + ";");
    }

    public static Type array(Type base) {
        return new Type("[" + base.desc());
    }

    public static Type array(Type base, int dims) {
        assert dims >= 0;
        if (dims == 0)
            return base;
        else if (dims == 1)
            return array(base);
        else
            return array(array(base), dims - 1);
    }

    public String toString() {
        return desc;
    }
}
