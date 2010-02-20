package ibex.types;

import ibex.lr.GLR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import polyglot.frontend.Source;
import polyglot.types.ClassDef_c;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.Predicate;

public class IbexClassDef_c extends ClassDef_c implements IbexClassDef {
    List<RuleDef> rules;
    GLR glr;
    
    protected IbexClassDef_c() {
        super();
    }

    public IbexClassDef_c(TypeSystem ts, Source fromSource) {
        super(ts, fromSource);
        this.rules = new ArrayList<RuleDef>();
    }

    public boolean isParser() {
        IbexTypeSystem ts = (IbexTypeSystem) this.ts;
        
        if (ts.isSubtype(asType(), ts.runtimeParserType(), ts.emptyContext())) {
            return true;
        }
        return false;
    }

    public GLR glr() {
        return glr;
    }
    
    public void setGLR(GLR glr) {
        this.glr = glr;
    }
    
    /* (non-Javadoc)
     * @see ibex.types.IbexClassDef#rules()
     */
    public List<RuleDef> rules() {
        return Collections.unmodifiableList(rules);
    }
    
    /* (non-Javadoc)
     * @see ibex.types.IbexClassDef#addRule(ibex.types.RuleDef)
     */
    public void addRule(RuleDef n) {
        rules.add(n);
    }
    
    /* (non-Javadoc)
     * @see ibex.types.IbexClassDef#setRules(java.util.List)
     */
    public void setRules(List<? extends RuleDef> l) {
        this.rules = new ArrayList<RuleDef>(l);
    }

}
