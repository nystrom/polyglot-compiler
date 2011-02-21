package polyglot.ext.jl5.types;

import java.util.Iterator;
import java.util.List;

import polyglot.types.ClassType_c;
import polyglot.types.Name;

public abstract class JL5ClassType_c extends ClassType_c implements JL5ClassType {

    public abstract List enumConstants();
    
    public EnumInstance enumConstantNamed(Name name){
        for(Iterator it = enumConstants().iterator(); it.hasNext();){
            EnumInstance ei = (EnumInstance)it.next();
            if (ei.name().equals(name)){
                return ei;
            }
        }
        return null;
    }

}
