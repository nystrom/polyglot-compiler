package polyglot.ext.jl5.types;

import polyglot.types.LazyClassInitializer_c;
import polyglot.types.TypeSystem;

public class JL5LazyClassInitializer_c extends LazyClassInitializer_c implements JL5LazyClassInitializer {

    public JL5LazyClassInitializer_c(TypeSystem ts){
        super(ts);
    }
    
    public void initEnumConstants(JL5ParsedClassType ct){
    }
    public void initAnnotations(JL5ParsedClassType ct){
    }
}
