package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ClassType;
import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Flags;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;

/**
 * Copy paste from ConstructorDef_c with support for typeVariables
 */
public class JL5ConstructorDef_c extends JL5ProcedureDef_c implements ConstructorDef, JL5ConstructorDef {

    /** Used for deserializing types. */
    protected JL5ConstructorDef_c() { }

    public JL5ConstructorDef_c(TypeSystem ts, Position pos,
	                         Ref<? extends ClassType> container,
				 Flags flags, List<Ref<? extends Type>> formalTypes, List<Ref<? extends Type>> throwTypes,
				 List<Ref<? extends Type>> typeVariableTypes) {
	        super(ts, pos, container, flags, formalTypes, throwTypes, typeVariableTypes);
    }
    
    protected transient ConstructorInstance asInstance;

    public ConstructorInstance asInstance() {
        if (asInstance == null) {
            asInstance = ts.createConstructorInstance(position(), Types.ref(this));
        }
        return asInstance;
    }

    public String toString() {
	return designator() + " " + flags.translate() + signature();
    }
    
    public String signature() {	
        return container + typeVariabletoString() + "(" + CollectionUtil.listToString(formalTypes) + ")";
    }

    public String designator() {
        return "constructor";
    }

}
