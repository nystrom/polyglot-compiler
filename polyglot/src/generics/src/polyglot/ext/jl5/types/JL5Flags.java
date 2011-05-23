package polyglot.ext.jl5.types;

import polyglot.types.Flags;

public class JL5Flags extends Flags {


    public static final int ANNOTATION_MOD = 8192;

	/** 
	 * ACC_ENUM 0x4000 Declared as an enum type.
	 * i.e. a class representing an enum or a field 
	 * representing an enum constant
	 */
    public static final int ENUM_MOD = 16384;

    public static final Flags VARARGS = createFlag("varargs", null);
    public static final Flags ENUM = createFlag("enum", null);
    public static final Flags ANNOTATION = createFlag("annotation", null);
    public JL5Flags(){
        super();
    }
     
    public static Flags setEnumModifier(Flags f){
        return f.set(ENUM);
    }

    public static Flags clearEnumModifier(Flags f){
        return f.clear(ENUM);
    }

    public static boolean isEnumModifier(Flags f){
        return f.contains(ENUM);
    }
    
    public static Flags setAnnotationModifier(Flags f){
        return f.set(ANNOTATION);
    }

    public static Flags clearAnnotationModifier(Flags f){
        return f.clear(ANNOTATION);
    }

    public static boolean isAnnotationModifier(Flags f){
        return f.contains(ANNOTATION);
    }
    
    /**
     * Return true if <code>this</code> has the <code>varargs</code> flag set.
     * Note that VARARGS and TRANSIENT have the same bit value.
     */
    public static boolean isVarargs(Flags f) {
    	return f.contains(VARARGS);
    }

}
