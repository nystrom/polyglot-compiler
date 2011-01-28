package polyglot.ext.jl5.types;

import polyglot.types.Flags;
import polyglot.types.MemberInstance;
import polyglot.types.ReferenceType;
import polyglot.types.Type;

public interface AnnotationElemInstance extends MemberInstance {
    public Flags flags();

    public Type type();

    public String name();

    public ReferenceType container();

    public boolean hasDefault();
}
