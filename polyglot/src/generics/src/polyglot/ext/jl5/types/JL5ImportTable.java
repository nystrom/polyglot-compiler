package polyglot.ext.jl5.types;
import java.util.ArrayList;
import java.util.List;

import polyglot.types.ImportTable;
import polyglot.types.QName;
import polyglot.types.Ref;
import polyglot.types.TypeSystem;
import  polyglot.types.Package;

public class JL5ImportTable extends ImportTable {

	/**
	 * JLS 7.5.3
	 * Reference to specific static member imported from a class 
	 */
    protected List<QName> staticExplicitImports;
    
    /**
     * JLS 7.5.4
     * Reference to classes which ALL static members are imported
     */
    protected List<QName> staticOnDemandImports;

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
    public void addExplicitStaticImport(QName memberName){
    	explicitStaticImports().add(memberName);
    }

    /**
     * import static TypeName . * ;
     * @param className
     */
    public void addOnDemandStaticImport(QName className){
    	onDemandStaticImports().add(className);
    }

    public List<QName> explicitStaticImports(){
    	if (this.staticExplicitImports == null) {
            this.staticExplicitImports = new ArrayList<QName>();
    	}
		return this.staticExplicitImports;
    }

    public List<QName> onDemandStaticImports(){
    	if (this.staticOnDemandImports == null) {
            this.staticOnDemandImports = new ArrayList<QName>();
    	}
		return this.staticOnDemandImports;
    }

//    //CHECK commented for further integration	
//    protected Named lookupExplicit(Matcher<Named> matcher, List<QName> explicitImportList,  
//    		List<Position> explicitImportPositionList) throws SemanticException {
//    	// CHECK this method should be moved to super and uncomment the Report code
//    	Set<QName> tried = new HashSet<QName>();
//
//    	for (int i = 0; i < explicitImportList.size(); i++) {
//    		QName longName = explicitImportList.get(i);
//    		Position pos = explicitImportPositionList.get(i);
//
//    		if (tried.contains(longName))
//    			continue;
//    		tried.add(longName);
//
////    		if (Report.should_report(TOPICS, 2))
////    			Report.report(2, this + ": import " + longName);
//
//    		if (longName.name().equals(matcher.name()))
//    			return findInContainer(matcher, longName.qualifier(), pos);
//    	}
//
//    	return null;
//    }

//    protected Named lookupExplicitStatic(Matcher<Named> matcher) throws SemanticException {
//      //CHECK commented for further integration
//    	return this.lookupExplicit(matcher, staticExplicitImports, staticExplicitImportsPosition);
//    }
//    
//    public Named find(Matcher<Named> matcher) throws SemanticException {
//      //CHECK commented for further integration	
//    	// Check in the cache
//    
//        Named result = null;
//        
//    	// Lookup Explicit static
//        for (Iterator<QName> it = staticExplicitImports.iterator(); it.hasNext(); ){
//        	QName next = it.next();
//            String id = StringUtil.getShortNameComponent(next);
//            if (name.equals(id)){
//                String className = StringUtil.getPackageComponent(next);
//                Named nt = ts.forName(className);
//                if (nt instanceof Type){
//                    Type t = (Type)nt;
//                    try {
//                        result = ts.findMemberClass(t.toClass(), name);
//                    }
//                    catch (SemanticException e){
//                    }
//                    if (result != null && ((ClassType)result).flags().isStatic()) return result;
//                }                                    
//            }
//        }
//
//        for (Iterator<QName> it = staticOnDemandImports.iterator(); it.hasNext(); ){
//        	QName next = it.next();
//            Named nt = ts.forName(next);
//            
//            if (nt instanceof Type){
//                Type t = (Type)nt;
//                try {
//                    result = ts.findMemberClass(t.toClass(), name);
//                }
//                catch(SemanticException e){
//                }
//                if (result != null && ((ClassType)result).flags().isStatic()) return result;
//            }
//        }
//
//        return super.find(matcher);
//    }
}
