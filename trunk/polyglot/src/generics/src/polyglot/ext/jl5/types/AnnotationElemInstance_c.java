package polyglot.ext.jl5.types;

import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.ReferenceType;
import polyglot.types.Type;
import polyglot.types.TypeObject_c;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class AnnotationElemInstance_c extends TypeObject_c implements AnnotationElemInstance {
    
    protected Flags flags;
    protected Type type;
    protected Name name;
    protected ClassType container;
    protected boolean hasDefault;
        
    public AnnotationElemInstance_c(TypeSystem ts, Position pos, ClassType ct, Flags flags, Type type, Name name, boolean hasDef){
        super(ts, pos);
        this.flags = flags;
        this.type = type;
        this.name = name;
        this.container = ct;
        this.hasDefault = hasDef;
    }

    public Flags flags(){
        return flags;
    }

    public Type type(){
        return type;
    }

    public Name name(){
        return name;
    }

    public boolean isCanonical(){
        return true;
    }

    public boolean hasDefault(){
        return hasDefault;
    }
}
