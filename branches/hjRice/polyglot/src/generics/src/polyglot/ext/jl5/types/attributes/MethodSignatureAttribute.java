package polyglot.ext.jl5.types.attributes;

import java.util.Collections;
import java.util.List;

import polyglot.types.DerefTransform;
import polyglot.types.MethodDef;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.util.TransformingList;

public class MethodSignatureAttribute {
    private Type returnType;
    private List<Type> formalTypes;
    private List<Type> thrownTypes;
    private List<Type> typeVariablesTypes;
    private List<List<Type>> boundsTypes;

    public MethodSignatureAttribute(MethodDef def) {
        returnType = def.returnType().get();
        formalTypes = new TransformingList<Ref<? extends Type>,Type>(def.formalTypes(), new DerefTransform<Type>());
        thrownTypes = new TransformingList<Ref<? extends Type>,Type>(def.throwTypes(), new DerefTransform<Type>());
        typeVariablesTypes = Collections.EMPTY_LIST;
        boundsTypes = Collections.EMPTY_LIST;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getFormalTypes() {
        return formalTypes;
    }

    public List<Type> getThrownTypes() {
        return thrownTypes;
    }

    public List<Type> getTypeVariablesTypes() {
        return typeVariablesTypes;
    }
    
    public List<List<Type>> getBoundsTypes() {
        return boundsTypes;
    }

}
