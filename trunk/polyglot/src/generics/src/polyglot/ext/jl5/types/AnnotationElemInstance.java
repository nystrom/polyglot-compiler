package polyglot.ext.jl5.types;

import polyglot.types.Flags;
import polyglot.types.MemberInstance;
import polyglot.types.Name;
import polyglot.types.ReferenceType;
import polyglot.types.Type;

public interface AnnotationElemInstance extends MemberInstance {
    public Flags flags();

    public Type type();

    public Name name();

    public boolean hasDefault();
}
