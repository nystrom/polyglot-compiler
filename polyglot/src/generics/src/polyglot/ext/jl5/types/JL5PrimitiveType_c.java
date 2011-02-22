package polyglot.ext.jl5.types;

import polyglot.types.Name;
import polyglot.types.PrimitiveType_c;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;

public class JL5PrimitiveType_c extends PrimitiveType_c implements JL5PrimitiveType, SignatureType {
   
    public JL5PrimitiveType_c(TypeSystem ts, Name name){
        super(ts, name);
    }


    public String signature(){
        if (this.isInt()) return "I";
        if (this.isByte()) return "B";
        if (this.isShort()) return "S";
        if (this.isChar()) return "C";
        if (this.isBoolean()) return "Z";
        if (this.isLong()) return "J";
        if (this.isDouble()) return "D";
        if (this.isFloat()) return "F";
        if (this.isVoid()) return "V";
        throw new RuntimeException("Unknown primitive type: "+this);
    }
}
