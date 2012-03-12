package polyglot.ext.jl5.types.attributes;

import java.util.LinkedList;
import java.util.List;

import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.DerefTransform;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.util.TransformingList;

public class ClassSignatureAttribute {
    private List<Type> typeVariablesTypes;
    private List<List<Type>> boundsTypes;
    private Type superType;
    private List<Type> interfacesTypes;
    
    public List<Type> getTypeVariablesTypes() {
        return typeVariablesTypes;
    }

    public List<Type> getInterfacesTypes() {
        return interfacesTypes;
    }

    public List<List<Type>> getBoundsTypes() {
        return boundsTypes;
    }

    public Type getSuperType() {
        return superType;
    }

    public ClassSignatureAttribute(List<Ref<? extends Type>> tvRefs, Type superRef, List<Type> interfacesTypes) {
        typeVariablesTypes = new TransformingList<Ref<? extends Type>,Type>(tvRefs, new DerefTransform<Type>());
        extractBounds(tvRefs);
        this.superType = superRef;
        this.interfacesTypes = interfacesTypes;
    }

    private void extractBounds(List<Ref<? extends Type>> tvRefs) {
        boundsTypes = new LinkedList<List<Type>>();
        for (Ref<? extends Type> ref : tvRefs) {
            TypeVariable tv = (TypeVariable)ref.get();
            boundsTypes.add(tv.bounds());
        }
    }
}
