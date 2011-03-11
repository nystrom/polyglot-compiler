package polyglot.ext.jl5.types;

import java.util.Collections;
import java.util.List;

import polyglot.types.Flags;
import polyglot.types.ProcedureDef_c;
import polyglot.types.Ref;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;
import polyglot.util.TypedList;

public abstract class JL5ProcedureDef_c extends ProcedureDef_c implements JL5ProcedureDef {

	List<Ref<? extends Type>> typeVariableTypes;
	
    /** Used for deserializing types. */
    protected JL5ProcedureDef_c() { }

    public JL5ProcedureDef_c(TypeSystem ts, Position pos,
			Ref<? extends StructType> container, Flags flags,
			List<Ref<? extends Type>> formalTypes,
			List<Ref<? extends Type>> throwTypes,
		    List<Ref<? extends Type>> typeVariableTypes) {
		super(ts, pos, container, flags, formalTypes, throwTypes);
		this.typeVariableTypes = typeVariableTypes;
	}

    
    protected String typeVariabletoString() {
    	String tv = "";
    	if (!typeVariableTypes().isEmpty()) {
    		tv = " <" + CollectionUtil.listToString(typeVariableTypes()) + "> ";
    	}
    	return tv;    	
    }
    

    /**
     * @param typeVariableTypes The typeVariableTypes to set.
     */
    public void setTypeVariableTypes(List<Ref<? extends Type>> typeVariableTypes) {
        this.typeVariableTypes = TypedList.copyAndCheck(typeVariableTypes, Ref.class, true);
    }
    
    public List<Ref<? extends Type>> typeVariableTypes() {
        return Collections.unmodifiableList(typeVariableTypes);
    }
}
