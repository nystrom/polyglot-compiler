package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.frontend.Source;
import polyglot.types.Flags;
import polyglot.types.LazyClassInitializer;
import polyglot.types.TypeSystem;

/* A reference to a generic type */

public abstract class GenericTypeRef_c extends JL5ParsedClassType_c implements GenericTypeRef {

    protected JL5ParsedClassType baseType;

    public GenericTypeRef_c(TypeSystem ts, LazyClassInitializer init, Source fromSource) {
        super(ts, init, fromSource);
    }

    public GenericTypeRef_c(JL5ParsedClassType t) {
        super(t.typeSystem(), t.init(), t.fromSource());
        this.baseType = t;
    }

    public Source fromSource() {
        return baseType.fromSource();
    }

    public Kind kind() {
        return baseType.kind();
    }

    public boolean inStaticContext() {
        return baseType.inStaticContext();
    }

    public String name() {
        return baseType.name();
    }

    public polyglot.types.Package package_() {
        return baseType.package_();
    }

    public Flags flags() {
        return baseType.flags();
    }

    /*
     * public ReferenceType toReference() { return this.baseType; }
     * 
     * public ClassType toClass(){ return this.baseType; }
     */

    public JL5ParsedClassType baseType() {
        return this.baseType;
    }


    public boolean isGeneric() {
        return baseType.isGeneric();
    }

    public List<TypeVariable> typeVariables() {
        return baseType.typeVariables();
    }

}
