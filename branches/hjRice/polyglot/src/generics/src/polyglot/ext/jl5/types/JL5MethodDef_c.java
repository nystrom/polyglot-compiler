package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.ext.jl5.types.attributes.MethodSignatureAttribute;
import polyglot.types.Flags;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;

/**
 * Copy paste from MethodDef_c with support for typeVariables
 */
public class JL5MethodDef_c extends JL5ProcedureDef_c implements MethodDef, JL5MethodDef {

    protected Name name;
    protected Ref<? extends Type> returnType;
    protected boolean compilerGenerated;
    protected MethodSignatureAttribute msa;
    
    /** Used for deserializing types. */
    protected JL5MethodDef_c() { }

    public JL5MethodDef_c(TypeSystem ts, Position pos,
	 		    Ref<? extends StructType> container,
	                    Flags flags, Ref<? extends Type> returnType, Name name,
			    List<Ref<? extends Type>> formalTypes, List<Ref<? extends Type>> throwTypes,
			    List<Ref<? extends Type>> typeVariableTypes, boolean compilerGenerated) {
        super(ts, pos, container, flags, formalTypes, throwTypes, typeVariableTypes);
	this.returnType = returnType;
	this.name = name;
	this.compilerGenerated = compilerGenerated;
    }
    
    protected transient MethodInstance asInstance;
    
    public MethodInstance asInstance() {
        if (asInstance == null) {
            asInstance = ts.createMethodInstance(position(), Types.ref(this));
        }
        return asInstance;
    }

    public Name name() {
        return name;
    }

    public Ref<? extends Type> returnType() {
        return returnType;
    }

    /**
     * @param name The name to set.
     */
    public void setName(Name name) {
        this.name = name;
        asInstance = null;
    }
    
    /**
     * @param returnType The returnType to set.
     */
    public void setReturnType(Ref<? extends Type> returnType) {
        this.returnType = returnType;
        asInstance = null;
    }

    public String toString() {
	String s = designator() + " " + flags.translate() + returnType + " " +
                   container() + "." + signature();

	if (! throwTypes.isEmpty()) {
	    s += " throws " + CollectionUtil.listToString(throwTypes);
	}

	return s;
    }

    public String signature() {
        return name + typeVariabletoString() +  "(" + CollectionUtil.listToString(formalTypes) + ")";
    }

    public String designator() {
        return "method";
    }

	@Override
	public boolean isCompilerGenerated() {
		return compilerGenerated;
	}

    @Override
    public void eraseWith(List<Ref<? extends Type>> formalsRefs, 
    		List<Ref<? extends Type>> thrownRefs, Ref<? extends Type> retRef) {
        msa = new MethodSignatureAttribute(this);
        this.setFormalTypes(formalsRefs);
        this.setThrowTypes(thrownRefs);
        this.setReturnType(retRef);
    }
    
    public MethodSignatureAttribute getSignatureAttr() {
        return msa;
    }

}
