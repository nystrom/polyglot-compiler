package polyglot.ext.jl5.types;

import polyglot.ext.jl.types.ArrayType_c;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class JL5ArrayType_c extends ArrayType_c implements JL5ArrayType, SignatureType {

    protected boolean  variable;

    public JL5ArrayType_c(TypeSystem ts, Position pos, Type base){
        super(ts, pos, base);
    }
    
    public void setVariable(){
        this.variable = true;
    }

    public boolean isVariable(){
        return this.variable;
    }

    public String signature(){
        return "["+((SignatureType)base).signature()+";";
    }
}
