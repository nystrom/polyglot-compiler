package polyglot.ext.jl5.types;
import java.util.ArrayList;
import java.util.List;

import polyglot.types.ImportTable;
import polyglot.types.Matcher;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Named;
import polyglot.types.NoClassException;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.TypeSystem_c.MethodMatcher;
import polyglot.util.Position;

public class JL5ImportTable extends ImportTable {

	/**
	 * JLS 7.5.3
	 * Reference to specific static member imported from a class 
	 */
    protected List<QName> staticExplicitImports;
    /** Parallel list of positions for lazyImports. */
    protected List<Position> staticExplicitImportsPositions;

    /**
     * JLS 7.5.4
     * Reference to classes which ALL static members are imported
     */
    protected List<QName> staticOnDemandImports;
    /** Parallel list of positions for lazyImports. */
    protected List<Position> staticOnDemandImportsPositions;

    public JL5ImportTable(TypeSystem ts, Ref<? extends Package> pkg) {
        this(ts, pkg, null);
    }
    
    public JL5ImportTable(TypeSystem ts, Ref<? extends Package> pkg, String src) {
        super(ts, (Ref<? extends polyglot.types.Package>) pkg, src);
    }

    /***
     * import static TypeName . Identifier;
     * @param memberName
     */
    public void addExplicitStaticImport(QName memberName, Position pos) {
        explicitStaticImports().add(memberName);
        explicitStaticImportsPosition().add(pos);
    }

    /**
     * import static TypeName . * ;
     * @param className
     */
    public void addOnDemandStaticImport(QName className, Position pos){
        onDemandStaticImports().add(className);
        onDemandStaticImportsPosition().add(pos);
    }

    public List<QName> explicitStaticImports(){
    	if (this.staticExplicitImports == null) {
            this.staticExplicitImports = new ArrayList<QName>();
    	}
		return this.staticExplicitImports;
    }

    public List<Position> explicitStaticImportsPosition(){
        if (this.staticExplicitImportsPositions == null) {
            this.staticExplicitImportsPositions = new ArrayList<Position>();
        }
        return this.staticExplicitImportsPositions;
    }
    
    public List<QName> onDemandStaticImports(){
    	if (this.staticOnDemandImports == null) {
            this.staticOnDemandImports = new ArrayList<QName>();
    	}
		return this.staticOnDemandImports;
    }

    public List<Position> onDemandStaticImportsPosition(){
        if (this.staticOnDemandImportsPositions == null) {
            this.staticOnDemandImportsPositions = new ArrayList<Position>();
        }
        return this.staticOnDemandImportsPositions;
    }

    /**
     * 
     * @param matcher
     * @return
     * @throws SemanticException if method call is ambiguous
     */
    public MethodInstance findMethod(MethodMatcher matcher) throws SemanticException {
        MethodInstance mi = null; 
        try {
            mi = lookupExplicitMethod(matcher);
        } catch(SemanticException e) {
        }

        if (mi == null) {
            // Couldn't find explicit, try on demand
            mi = lookupOnDemandMethod(matcher);
        }
        return mi;
    }

    protected MethodInstance lookupExplicitMethod(MethodMatcher matcher) throws SemanticException {
        MethodInstance mi = null;
        for (QName staticName : explicitStaticImports()) {
            Name mthNameCandidate = staticName.name();
            // match a static import name, this should be it
            if(mthNameCandidate.equals(matcher.name())) {
                QName className = staticName.qualifier();
                mi = ts.findMethod(ts.typeForName(className), matcher);
            }
        }
        return mi;
    }

    protected MethodInstance lookupOnDemandMethod(MethodMatcher matcher) throws SemanticException  {
        MethodInstance mi = null;

        //lookup on-demand imports
        for (QName staticName : onDemandStaticImports()) {
            MethodInstance res = null;
            try {
                res = ts.findMethod(ts.typeForName(staticName), matcher);
                if (mi == null) {
                    // First time we found a mi matching
                    mi = res;
                    res = null;
                }
            } catch (SemanticException e) {
                // do not report errors since it's on-demand
                continue;
            }
            if ((mi != null) && (res != null)) {
                throw new SemanticException("The static method call '" + mi.name() + "' is ambiguous");
            }
        }
        return mi;
    }
  
    public Named find(Matcher<Named> matcher) throws SemanticException {
        try {
            Named n = super.find(matcher);
            return n;
        } catch (NoClassException e) {
            Named exp = null;
            try { 
                exp = lookupStaticExplicit(matcher);
                if (exp != null) {
                    return exp;
                }
            } catch (NoClassException ee) {            
            }
            return lookupOnDemandStatic(matcher);
        }
    }
    
    protected Named lookupStaticExplicit(Matcher<Named> matcher) throws SemanticException {
        return lookupExplicitImpl(matcher, explicitStaticImports(), explicitStaticImportsPosition());
    }
    
    
    protected Named lookupOnDemandStatic(Matcher<Named> matcher) throws SemanticException, NoClassException {
        return lookupOnDemandImpl(matcher, onDemandStaticImports(), onDemandStaticImportsPosition(), false);
    }
    
    public boolean hasExplicitName(Name importName) {
        for (QName explicitImport : explicitStaticImports()) {
            Name explicitName = explicitImport.name();
            if (explicitName.equals(importName)) {
                return true;
            }
        }
        return false;
        
    }

}
