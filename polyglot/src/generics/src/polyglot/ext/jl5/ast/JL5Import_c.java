package polyglot.ext.jl5.ast;

import java.util.Iterator;
import java.util.List;

import polyglot.ast.Import;
import polyglot.ast.Import_c;
import polyglot.ast.Node;
import polyglot.ext.jl5.types.JL5ImportTable;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.Named;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.util.StringUtil;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;
import polyglot.visit.TypeBuilder;

/**
 * Add support for JLS:
 * 7.5.3 Single Static Import Declaration
 * 7.5.4 Static-Import-on-Demand Declaration
 */
public class JL5Import_c extends Import_c implements JL5Import{
    protected boolean isStaticImport; 

    public JL5Import_c(Position pos, Import.Kind kind, QName name, boolean isStatic){
        super(pos, kind, name);
        this.isStaticImport = isStatic;
        if (isStaticImport) {
        	System.out.println("Static import not yet supported");
        	assert(false);
        }
    }

//  //CHECK commented for further integration
//    public Node buildTypes(TypeBuilder tb) throws SemanticException {
//    	
//    	if (isStaticImport) {
//            JL5ImportTable it = (JL5ImportTable)tb.importTable();
//            if (kind() == JL5Import.STATIC_MEMBER){
//                it.addExplicitStaticImport(name);
//            }
//            else if (kind() == JL5Import.ALL_STATIC_MEMBERS){
//            	// why all_members means we add a static class import ?
//                it.addOnDemandStaticImport(name);
//            }
//            
//            // beside MEMBER / ALL_MEMBERS there are no indications of
//            // whether this is a static or regular import
//            return this;    		
//    	} else {
//    		return super.buildTypes(tb);
//    	}
//    }
//
//    public Node typeCheck(ContextVisitor tc) throws SemanticException {
//        TypeSystem ts = tc.typeSystem();
//        if (isStaticImport) {
//            // check package exists
//
//        	// The TypeName must be the canonical name of a class or interface type; 
//        	// a compile-time error occurs if the named type does not exist. 
//        	
//        	// The named type must be accessible (¤6.6) or a compile-time error occurs. 
//        	
//        	// The Identifier must name at least one static member of the named type; 
//        	// a compile-time error occurs if there is no member of that name or if all 
//        	// of the named members are not accessible.
//        	
//            // this just check first part of the package !
//            String pkgName = StringUtil.getFirstComponent(name.toString);
//            if (! ts.systemResolver().packageExists(pkgName)){
//                throw new SemanticException("Package \"" + pkgName +
//                    "\" not found.", position());
//            }
//
//            // check class exists and is accessible
//            Named nt;
//            if (kind() == JL5Import.STATIC_MEMBER){
//            	// This time really get the package component part
//                nt = tc.typeSystem().forName(StringUtil.getPackageComponent(name));
//            }
//            else {
//            	// assume ALL_MEMBER here ?
//            	// or just get the name ?? (i.e. is that just a classname ?)
//                nt = tc.typeSystem().forName(name);
//            }
//            
//            // what could it be beside a Type ?
//            if (nt instanceof Type){
//                Type t = (Type) nt;
//                if (t.isClass()){
//                    tc.typeSystem().classAccessibleFromPackage(t.toClass(), 
//                        tc.context().package_());
//                    // if member check class contains some static member by the 
//                    // given name
//                    if (kind() == JL5Import.STATIC_MEMBER){
//                    	// get the class name 
//                        String id = StringUtil.getShortNameComponent(name);
//                        // Sounds that everything in MEMBER should be static
//                        if (!isIdStaticMember(t.toClass(), id, tc.typeSystem())){
//                            throw new SemanticException("Cannot import: "+id+" from class: "+t, position());
//                        }
//                    }
//                } else {
//                	//CHECK sanity check here what could it be beside a Class ?
//                	assert(false);            	
//                }
//            } else {
//            	//CHECK sanity check here what could it be beside a Type ?
//            	assert(false);
//            }
//
//            //findStaticMemberImportCollisions(tc);
//            
//            return this; 
//        	
//        } else {
//        	return super.typeCheck(tc);
//        }
//    }
//
//    private void findStaticMemberImportCollisions(ContextVisitor tc) throws SemanticException {
//    	//CHECK This code is never used
//    	if (kind() == JL5Import.STATIC_MEMBER){
//            String id = StringUtil.getShortNameComponent(name.toString());
//            List l = ((JL5ImportTable)tc.context().importTable()).explicitStaticImports();
//            for (Iterator it = l.iterator(); it.hasNext(); ){
//                String next = (String)it.next();
//                String nextId = StringUtil.getShortNameComponent(next);
//                //if (nextId.equals(id) && !next.equals(name)){
//                if (next.equals(name)){
//                    throw new SemanticException("The import statement "+this+" collides with another import statement.", position());
//                }
//            }
//        }
//    }
//    
//    private boolean isIdStaticMember(ClassType t, String id, TypeSystem ts){
//        try {
//            FieldInstance fi = ts.findField(t, id);
//            if (fi != null && fi.flags().isStatic()) return true;
//        }
//        catch(SemanticException e){}
//
//        if (ts.hasMethodNamed(t, id)) return true;
//
//        try {
// CHECK Should be replace by something like 
// context.findInThisScope(ts.TypeMatcher(name));
// No idea how we're supposed to get a context around here though...
//            ClassType ct = ts.findMemberClass(t, id);
//            if (ct != null && ct.flags().isStatic()) return true;
//        }
//        catch(SemanticException e){}
//
//        return false;
//    }
//
//    public String toString(){
//        return "import static "+name + (kind() == ALL_MEMBERS ? ".*": "");
//    }
//
//    public void prettyPrint(CodeWriter w, PrettyPrinter tr){
//        w.write("import static ");
//        w.write(name);
//
//        if (kind() == ALL_MEMBERS){
//            w.write(".*");
//        }
//
//        w.write(";");
//        w.newline(0);
//    }
}
