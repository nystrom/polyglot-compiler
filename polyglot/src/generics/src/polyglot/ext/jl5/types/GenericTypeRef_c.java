package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.frontend.Source;
import polyglot.types.ClassDef;
import polyglot.types.ClassDef.Kind;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.Ref;

/* A reference to a generic type */

public abstract class GenericTypeRef_c extends JL5ParsedClassType_c implements GenericTypeRef {

    protected JL5ParsedClassType baseType;

    public GenericTypeRef_c(JL5ParsedClassType t) {
        super(t.def());
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

    public Name name() {
        return baseType.name();
    }

    public polyglot.types.Package package_() {
        return baseType.package_();
    }

    public Flags flags() {
        return baseType.flags();
    }

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
