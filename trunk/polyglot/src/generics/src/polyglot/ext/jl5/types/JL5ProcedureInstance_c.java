package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.types.ProcedureInstance_c;
import polyglot.types.Flags;
import polyglot.types.ReferenceType;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.UnknownType;
import polyglot.util.Position;

public abstract class JL5ProcedureInstance_c extends ProcedureInstance_c implements JL5ProcedureInstance {
    
    public JL5ProcedureInstance_c() {
        super();
        // TODO Auto-generated constructor stub
    }

    public JL5ProcedureInstance_c(TypeSystem ts, Position pos, ReferenceType container, Flags flags, List formalTypes, List excTypes) {
        super(ts, pos, container, flags, formalTypes, excTypes);
        // TODO Auto-generated constructor stub
        typeVariables = new ArrayList<TypeVariable>();
    }

    protected List<TypeVariable> typeVariables;
    protected List<TypeVariable> substTypeVariables;
    
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

    public boolean hasTypeVariable(String name){
        for (Iterator it = typeVariables.iterator(); it.hasNext(); ){
            TypeVariable iType = (TypeVariable)it.next();
            if (iType.name().equals(name)) return true;
        }
        return false;
    }

    public TypeVariable getTypeVariable(String name){
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
        if ((typeVariables != null) && !typeVariables.isEmpty()) return true;
        return false;
    }

    public boolean isVariableArrity() {
        int numFormals;
        if ((numFormals = formalTypes().size()) > 0) {
            Type last = (Type) formalTypes().get(numFormals-1);
            if (last instanceof JL5ArrayType) {
                JL5ArrayType lastAsArr = (JL5ArrayType) last;
                if (lastAsArr.isVariable()) return true;
            }
        }
        return false;
    }

    @Override
    public boolean callValidImpl(List argTypes) {
        JL5TypeSystem ts = (JL5TypeSystem)typeSystem();
        List<Type> l1 = this.formalTypes();
        List<Type> l2 = argTypes;
        if ((l1.size() == 0) && (l2.size() != 0)) return false;
        
        Iterator<Type> it1 = l1.iterator();
        Iterator<Type> it2 = l2.iterator();
        
        //FIXME variable arrity methods!!!!!
        while (it1.hasNext() && it2.hasNext()) {
            Type t1 = it1.next();
            Type t2 = it2.next();
            
            if (it1.hasNext()) {//not last formal parameter
                if (! ts.isImplicitCastValid(t2, t1)) return false;
            }
            else if (t1.isArray() && ((JL5ArrayType)t1).isVariable()) {
                JL5ArrayType vartype = (JL5ArrayType)t1;
                if (!it2.hasNext()) {
                    return ts.isImplicitCastValid(t2, vartype) || ts.isImplicitCastValid(t2, vartype.base());
                }
                else while (ts.isImplicitCastValid(t2, vartype.base())) { //eat up actual args
                    if (it2.hasNext()) {
                        t2 = it2.next();
                    } else break;
                }
            }
            else {
                if (! ts.isImplicitCastValid(t2, t1)) {
		    return false;
		}
            }
        }
        if (it1.hasNext() && isVariableArrity()) it1.next();
        return ! (it1.hasNext() || it2.hasNext());
    }

    
    protected List<Type> typeArguments;
    
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

    public JL5ProcedureInstance erasure() {
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

    public boolean hasSameFormals(JL5ProcedureInstance other) {
        if (isGeneric() != other.isGeneric()) return false;
        if (formalTypes().size() != other.formalTypes().size()) return false;
        if (isGeneric() && typeVariables().size() != other.typeVariables().size()) return false;

        JL5ProcedureInstance subst_other = other.typeArguments(this.typeVariables());
        for (int i = 0; i < this.typeVariables().size(); i++) {
            IntersectionType bound1 = this.typeVariables().get(i).upperBound();
            IntersectionType bound2 = subst_other.typeVariables().get(i).upperBound();
            if (!ts.equals(bound1, bound2)) return false;
        }
        for (int i = 0; i < this.formalTypes().size(); i++) {
            Type formal1 = this.formalTypes().get(i);
            Type formal2 = subst_other.formalTypes().get(i);
            if (!ts.equals(formal1, formal2)) return false;
        }

        return true;
    }
    
    
    
    //FIXME implement moreSpecific!!!!
    
    
    
    
}
