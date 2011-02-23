package polyglot.ext.jl5.types;

import polyglot.types.Flags;

public class JL5Flags extends Flags {

    public static final int ANNOTATION_MOD = 8192;
    public static final int ENUM_MOD = 16384;

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
}
