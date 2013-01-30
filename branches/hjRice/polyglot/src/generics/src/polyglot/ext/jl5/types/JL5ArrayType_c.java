package polyglot.ext.jl5.types;

import polyglot.types.ArrayType_c;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class JL5ArrayType_c extends ArrayType_c implements JL5ArrayType, SignatureType {

    protected final boolean varArgs;

    public JL5ArrayType_c(TypeSystem ts, Position pos, Ref<? extends Type> base) {
        this(ts, pos, base, false);
    }
    
    public JL5ArrayType_c(TypeSystem ts, Position pos, Ref<? extends Type> base, boolean varArgs) {
        super(ts, pos, base);
        this.varArgs = varArgs;
    }

    /**
     * This is used by reflection ?
     */
    public boolean isVarargs(){
        return this.varArgs;
    }

    public String signature(){
        return "["+((SignatureType)base).signature()+";";
    }
    
    public boolean isTypeVariable() {
        return false;
    }
}
