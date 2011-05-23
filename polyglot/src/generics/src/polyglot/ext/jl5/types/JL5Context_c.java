package polyglot.ext.jl5.types;

import java.util.HashMap;
import java.util.Map;

import polyglot.types.Context_c;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.VarInstance;

public class JL5Context_c extends Context_c implements JL5Context {

    protected Map<Name, Ref<? extends Type>> typeVars;

    protected TypeVariable typeVariable;

    public static final Kind TYPE_VAR = new Kind("type-var");

    public JL5Context_c(TypeSystem ts) {
        super(ts);
    }

    public JL5TypeSystem typeSystem() {
        return (JL5TypeSystem) ts;
    }

    public VarInstance findVariableInThisScope(Name name) {
        VarInstance vi = super.findVariableInThisScope(name);
        if (vi == null && isClass()) {
            try {
            	JL5TypeSystem jts = (JL5TypeSystem) ts;
                return jts.findEnumConstant(this.currentClass(), jts.EnumMatcher(this.currentClass(), name, this));
            } catch (SemanticException e) {
                return null;
            }
        }
        return vi;
    }

    public VarInstance findVariableSilent(Name name) {
        VarInstance vi = findVariableInThisScope(name);
        if (vi != null) {
            return vi;
        }

//    	//CHECK Commented because we do not support static import yet
//        try {
//            // might be static
//            if (importTable() != null) {
//                JL5ImportTable jit = (JL5ImportTable) importTable();
//                for (Iterator it = jit.explicitStaticImports().iterator(); it.hasNext();) {
//                    String next = (String) it.next();
//                    String id = StringUtil.getShortNameComponent(next);
//                    if (name.equals(id)) {
//                        Named nt = ts.forName(StringUtil.getPackageComponent(next));
//                        if (nt instanceof Type) {
//                            Type t = (Type) nt;
//                            try {
//                                vi = ts.findField(t.toClass(), name);
//                            } catch (SemanticException e) {
//                            }
//                            if (vi != null) {
//                                return vi;
//                            }
//                        }
//                    }
//                }
//                if (vi == null) {
//                    for (Iterator it = jit.onDemandStaticImports().iterator(); it.hasNext();) {
//                        String next = (String) it.next();
//                        Named nt = ts.forName(next);
//                        if (nt instanceof Type) {
//                            Type t = (Type) nt;
//                            try {
//                                vi = ts.findField(t.toClass(), name);
//                            } catch (SemanticException e) {
//                            }
//                            if (vi != null)
//                                return vi;
//                        }
//                    }
//                }
//            }
//        } catch (SemanticException e) {
//        }

        if (outer != null) {
            return outer.findVariableSilent(name);
        }
        return null;
    }

    /*
     * June 05, 2005 - change Type variables are bound to class declaration, and
     * therefore should not be searched. Any typevariables visible are
     * explicitly added to context so this search is wrong.
     * 
     * public Named findInThisScope(String name){ if (types != null){ for
     * (Iterator typesIt = types.keySet().iterator(); typesIt.hasNext();){
     * String nextType = (String)typesIt.next(); Named next =
     * (Named)types.get(nextType); if (next instanceof JL5ParsedClassType &&
     * ((JL5ParsedClassType)next).isGeneric()){ JL5ParsedClassType ct =
     * (JL5ParsedClassType)next; if (ct.hasTypeVariable(name)){ return
     * ct.getTypeVariable(name); } } } } return super.findInThisScope(name); }
     */
    /*
     * public MethodInstance findMethod(String name, List argTypes) throws
     * SemanticException { if (this.currentClass() != null &&
     * ts.hasMethodNamed(this.currentClass(), name)){ return
     * ts.findMethod(this.currentClass(), name, argTypes, this.currentClass()); }
     * 
     * if (importTable() != null){ JL5ImportTable jit =
     * (JL5ImportTable)importTable(); for (Iterator it =
     * jit.memberImports().iterator(); it.hasNext(); ){ String next =
     * (String)it.next(); String id = StringUtil.getShortNameComponent(next); if
     * (name.equals(id)){ Named nt =
     * ts.forName(StringUtil.getPackageComponent(next)); if (nt instanceof
     * Type){ Type t = (Type)nt; if (t.isClass() &&
     * ts.hasMethodNamed(t.toClass(), name)){ return ts.findMethod(t.toClass(),
     * name, argTypes, this.currentClass()); } } } }
     * 
     * for (Iterator it = jit.staticClassImports().iterator(); it.hasNext(); ){
     * String next = (String)it.next(); Named nt = ts.forName(next); if (nt
     * instanceof Type){ Type t = (Type)nt; if (t.isClass() &&
     * ts.hasMethodNamed(t.toClass(), name)){ return ts.findMethod(t.toClass(),
     * name, argTypes, this.currentClass()); } } } } if (outer != null){ return
     * outer.findMethod(name, argTypes); }
     * 
     * throw new SemanticException("Method "+name+" not found."); }
     * 
     */

    protected Context_c push() {
        Context_c v = super.push();// (Context_c) this.copy();
        // v.outer = this;
        // v.types = null;
        // v.vars = null;
        ((JL5Context_c) v).typeVars = null;
        return v;
    }

    public JL5Context pushTypeVariable(TypeVariable iType) {
        JL5Context_c v = (JL5Context_c) push();
        v.typeVariable = iType;
        v.kind = TYPE_VAR;
        // v.outer = this;
        return v;
    }

    public TypeVariable findTypeVariableInThisScope(Name name) {
        if (typeVariable != null && typeVariable.name().equals(name))
            return typeVariable;
        if (typeVars != null && typeVars.containsKey(name)) {
            return (TypeVariable) typeVars.get(name).get();
        }
        if (outer != null) {
        	//CHECK the method name suggests we shouldn't recurse on outer
            return ((JL5Context) outer).findTypeVariableInThisScope(name);
        }
        return null;
    }

    /**
     * @deprecated
     */
    public boolean inTypeVariable() {
        return kind == TYPE_VAR;
    }

    public String toString() {
        return super.toString() + "type var: " + typeVariable;
    }

    public JL5Context addTypeVariable(Name name, Ref<? extends Type> type) {
    	assert(type != null);
        if (typeVars == null) {
            typeVars = new HashMap<Name, Ref<? extends Type>>();
        }
        typeVars.put(name, type);
        return this;
    }

}
