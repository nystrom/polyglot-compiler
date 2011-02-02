package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.List;

import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Flags;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;

public class JL5ConstructorInstance_c extends JL5ProcedureInstance_c implements JL5ConstructorInstance {
 
    public JL5ConstructorInstance_c(TypeSystem ts, Position pos, ClassType container, Flags flags, List formals, List excTypes){
        super(ts, pos, container, flags, formals, excTypes);
    }
    /** Used for deserializing types. */
    protected JL5ConstructorInstance_c() { }

    public JL5ConstructorInstance flags(Flags flags) {
        if (!flags.equals(this.flags)) {
            JL5ConstructorInstance_c n = (JL5ConstructorInstance_c) copy();
            n.flags = flags;
            return n;
        }
        return this;
    }

    public JL5ConstructorInstance formalTypes(List l) {
        if (!CollectionUtil.allEqual(this.formalTypes, l)) {
            JL5ConstructorInstance_c n = (JL5ConstructorInstance_c) copy();
            n.formalTypes = new ArrayList(l);
            return n;
        }
        return this;
    }

    public JL5ConstructorInstance throwTypes(List l) {
        if (!CollectionUtil.allEqual(this.excTypes, l)) {
            JL5ConstructorInstance_c n = (JL5ConstructorInstance_c) copy();
            n.excTypes = new ArrayList(l);
            return n;
        }
        return this;
    }

    public JL5ConstructorInstance container(ClassType container) {
        if (this.container != container) {
            JL5ConstructorInstance_c n = (JL5ConstructorInstance_c) copy();
            n.container = container;
            return n;
        }
        return this;
    }

    public String toString() {
    return designator() + " " + flags.translate() + signature();
    }
    
    public String signature() {
        return container + "(" + JL5TypeSystem_c.listToString(formalTypes) + ")";
    }

    public String designator() {
        return "constructor";
    }

    public boolean equalsImpl(TypeObject o) {
        if (! (o instanceof ConstructorInstance) ) return false;
        return super.equalsImpl(o);
    }

    public boolean isCanonical() {
    return container.isCanonical()
        && listIsCanonical(formalTypes)
        && listIsCanonical(excTypes);
    }
}
