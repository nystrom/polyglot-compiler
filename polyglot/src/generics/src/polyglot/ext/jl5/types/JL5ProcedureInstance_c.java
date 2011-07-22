package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ext.jl5.types.JL5TypeSystem_c.TypeVariableEquals;
import polyglot.types.Context;
import polyglot.types.Name;
import polyglot.types.ProcedureDef;
import polyglot.types.ProcedureInstance_c;
import polyglot.types.Ref;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.TypeSystem_c.TypeEquals;
import polyglot.types.UnknownType;
import polyglot.util.CollectionUtil;
import polyglot.util.Position;

public abstract class JL5ProcedureInstance_c<T extends ProcedureDef> extends ProcedureInstance_c<T> implements JL5ProcedureInstance<T> {

    protected List<TypeVariable> typeVariables;
    protected List<TypeVariable> substTypeVariables;
    protected List<Type> typeArguments;

	public JL5ProcedureInstance_c(TypeSystem ts, Position pos, Ref<? extends T> def) {
        super(ts, pos, def);
        typeVariables = new ArrayList<TypeVariable>();
    }

	
	public JL5ProcedureInstance<T> formalTypes(List<Type> formalTypes) {
		return (JL5ProcedureInstance<T>) super.formalTypes(formalTypes);
	}

	public JL5ProcedureInstance<T> throwTypes(List<Type> throwTypes) {
		return (JL5ProcedureInstance<T>) super.throwTypes(throwTypes);
	}
	
    public List<TypeVariable> typeVariables(){
        if (typeArguments == null || typeArguments.size() == 0)
            return typeVariables;
        if (substTypeVariables != null) 
            return substTypeVariables;
        List<TypeVariable> r =  new ArrayList<TypeVariable>();
        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        for (TypeVariable tv : typeVariables) {
            TypeVariable n = (TypeVariable) tv.copy();
            n.bounds(ts.applySubstitution(tv.bounds(), typeVariables, typeArguments));
            r.add(n);
        }
        return (substTypeVariables = r);
    }

    public void addTypeVariable(TypeVariable type){
        if (typeVariables == null){
            typeVariables = new ArrayList<TypeVariable>();
        }
        typeVariables.add(type);
        type.declaringProcedure(this);
    }

    public boolean hasTypeVariable(Name name){
    	return getTypeVariable(name) != null;
    }

    public TypeVariable getTypeVariable(Name name){
        for (Iterator it = typeVariables.iterator(); it.hasNext(); ){
            TypeVariable iType = (TypeVariable)it.next();
            if (iType.name().equals(name)) return iType;
        }
        return null;
    }

    public void typeVariables(List<TypeVariable> vars){
        typeVariables = vars;
        for (TypeVariable tv : typeVariables) {
            tv.declaringProcedure(this);
        }
    }
    
    public boolean isGeneric(){
        return ((typeVariables != null) && !typeVariables.isEmpty());
    }

    public boolean isGeneric(List<TypeVariable> l){
        return ((l != null) && !l.isEmpty());
    }

    
    public boolean isVariableArrity() {
        int numFormals;
        if ((numFormals = formalTypes().size()) > 0) {
            Type last = (Type) formalTypes().get(numFormals-1);
            if (last instanceof JL5ArrayType) {
                JL5ArrayType lastAsArr = (JL5ArrayType) last;
                if (lastAsArr.isVarargs()) return true;
            }
        }
        return false;
    }

    public boolean callValid(Type thisType, List<Type> argTypes, Context context) {
    	JL5TypeSystem ts = (JL5TypeSystem)typeSystem();
    	List<Type> l1 = this.formalTypes();
    	List<Type> l2 = argTypes;
    	if ((l1.size() == 0) && (l2.size() != 0)) return false;

    	Iterator<Type> itCallee = l1.iterator();
    	Iterator<Type> itCaller = l2.iterator();

    	// caller can either:
    	// - have one argument less than callee (last argument of 
    	//   callee is a varargs and caller do not provide a value)
    	// - have same number of args. The last arg being either
    	//		- same type as the last arg of callee
    	//		- or same type as the last arg of callee which is a varargs array
    	//		- or an array of same type as the last arg of callee (which could be a varargs array)
    	// - have more args, then:
    	//		- last args of callee must be a varargs array
    	//		- all extra args provided by the caller must match
    	//        the varargs array type of the callee.
    	
    	while (itCallee.hasNext() && itCaller.hasNext()) {
    		Type t1 = itCallee.next();
    		Type t2 = itCaller.next();

    		// Varargs can be used only in the final argument position
    		// When we reach the final argument, we check if it is varargs array.
    		if (!itCallee.hasNext() && t1.isArray() && ((JL5ArrayType)t1).isVarargs()) {
    			JL5ArrayType vartype = (JL5ArrayType)t1;
    			// Every arguments remaining in the second iterator must match the type
    			// of the varargs array
    			
    			if (!itCaller.hasNext()) {
    				// if we also reached the last element of the caller, 
    				// check if the type matches or if it is an array
    				return ts.isImplicitCastValid(t2, vartype, context) || ts.isImplicitCastValid(t2, vartype.base(), context);
    			} else {
    				// There are several arguments left, they should all match the callee's varargs array type.
    				while (itCaller.hasNext()) { //eat up actual args
    					if (!ts.isImplicitCastValid(t2, vartype.base(), context)) {
    						return false;
    					}
    					t2 = itCaller.next();
    				}
    			}
    		} else {
				if (!ts.isImplicitCastValid(t2, t1, context)) {
					return false;
				}
    		}
    	}
    	// Caller provided less args than the callee has, which is legal
    	// if callee is a variable arity method
    	if (itCallee.hasNext() && isVariableArrity()) { 
    		itCallee.next();
    	}

    	// and we've reached callee's last arg.
    	return ! (itCallee.hasNext() || itCaller.hasNext());
    }

    
    public List<Type> typeArguments() {
        return typeArguments;
    }

    //return a copy of this, since we don't want to polute the method and constructor declarations in classes.
    //typeArguments are used only when dealing with actual calls.
    public JL5ProcedureInstance typeArguments(List<? extends Type> typeArgs) {
        JL5ProcedureInstance_c n = (JL5ProcedureInstance_c) this.copy();
        n.typeArguments = new ArrayList<Type>();
        n.typeArguments.addAll(typeArgs);
        return n;
    }
    
    /**
     * 
     * @return type arguments where each unknown type argument is replaced with declared type variable
     */
    protected List<Type> knownTypeArguments() {
        List<Type> typeArgsForSub = new ArrayList<Type>(typeArguments);
        for (int i = 0; i < typeArgsForSub.size(); i++) {
            if (typeArgsForSub.get(i)==null || typeArgsForSub.get(i) instanceof UnknownType) {
                typeArgsForSub.set(i, typeVariables().get(i));
            }
        }
        return typeArgsForSub;
    }
    
    protected List<Type> substFormals = null; 
    
    public List<Type> formalTypes() {
        List<Type> declaredFormals = super.formalTypes();
        if (!isGeneric() || typeArguments == null) 
            return declaredFormals;
        if (substFormals != null) 
            return substFormals;
        JL5TypeSystem ts = (JL5TypeSystem)typeSystem();
        List<Type> r = new ArrayList<Type>();
        for (Type t : declaredFormals) {
           r.add(ts.applySubstitution(t, typeVariables, knownTypeArguments()));
        }
        return (substFormals = r);
    }

    public JL5ProcedureInstance<T> erasure() {
        JL5TypeSystem ts = (JL5TypeSystem) typeSystem();
        JL5ProcedureInstance_c n = (JL5ProcedureInstance_c) this.copy();
        List<Type> erasedFormals = new ArrayList<Type>();
        for (Type formal : formalTypes()) {
            erasedFormals.add(ts.erasure(formal));
        }
        n.formalTypes = erasedFormals;
        n.typeArguments = null;
        n.substTypeVariables = null;
        return n;
    }


    /** Returns true if the procedure has the given formal parameter types. */
    public boolean hasFormals(List<Type> otherFormalTypes, List<TypeVariable> otherTypeVariables, List<Type> typeArguments, Context context) {
        if (isGeneric() != isGeneric(otherTypeVariables)) return false;
        if (formalTypes().size() != otherFormalTypes.size()) return false;
        if (isGeneric() && typeVariables().size() != otherTypeVariables.size()) return false;

        if (!CollectionUtil.allElementwise(this.typeVariables(), otherTypeVariables, new TypeVariableEquals(context))) {
        	return false;
        }

        if (!CollectionUtil.allElementwise(this.formalTypes(), otherFormalTypes, new TypeEquals(context))) {
        	return false;
        }

        return true;
    }
    
}
