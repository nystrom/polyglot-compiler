package polyglot.ext.jl5.types.attributes;

import java.util.List;

import polyglot.types.DerefTransform;
import polyglot.types.MethodDef;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.util.TransformingList;

public class MethodSignatureAttribute {
    private Type returnType;
    private List<Type> formalTypes;
    
    public MethodSignatureAttribute(MethodDef def) {
        returnType = def.returnType().get();
        formalTypes = new TransformingList<Ref<? extends Type>,Type>(def.formalTypes(), new DerefTransform<Type>());        
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getFormalTypes() {
        return formalTypes;
    }
   
}
