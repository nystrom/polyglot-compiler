package polyglot.ext.jl5.ast;

import java.util.List;
import java.util.StringTokenizer;

import polyglot.ast.Import;
import polyglot.ast.Import_c;
import polyglot.ast.Node;
import polyglot.frontend.Globals;
import polyglot.types.ClassType;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Named;
import polyglot.types.QName;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;
import polyglot.visit.PrettyPrinter;

/**
 * Add support for JLS:
 * 7.5.3 Single Static Import Declaration
 * 7.5.4 Static-Import-on-Demand Declaration
 */
public class JL5Import_c extends Import_c implements JL5Import{

    public JL5Import_c(Position pos, Import.Kind kind, QName name){
        super(pos, kind, name);
    }

    public boolean isStaticImport() {
        return ((kind == ALL_STATIC_MEMBERS) || (kind == STATIC_MEMBER));
    }

    /**
     * @param ts
     * @param name A fully qualified name
     * @return The package part of 'name' 
     */
    public QName findPackage(TypeSystem ts, QName name) {
        while((name != null) && !ts.packageExists(name)) {
            name = name.qualifier();
        }
        return name;
    }

    /**
     * 
     * @param cv
     * @param importName
     * @throws SemanticException if import is not found or not accessible
     */
    protected void checkStaticImport(ContextVisitor cv) throws SemanticException {
        TypeSystem ts = cv.typeSystem();
        Context ctx = cv.context();
        // Try to resolve qualifier
        QName pkg = findPackage(ts, name);
        String pkgStr = pkg.toString();
        String impStr = name.toString();

        // Get what's remaining after the package name
        QName staticImport = QName.make(impStr.substring(pkgStr.length(), impStr.length()));
        StringTokenizer stoken = new StringTokenizer(staticImport.toString(), ".");
        String className = stoken.nextToken();
        QName currentName = QName.make(pkg, Name.make(className));

        // first element after the package must be a class
        Named resolved = ts.systemResolver().find(currentName);
        assert (resolved instanceof ClassType);
        ClassType ct = (ClassType) resolved;
        // The named type must be accessible (¤6.6) or a compile-time error occurs.
        if (! ts.classAccessibleFromPackage(ct.def(), ctx.package_())) {
            throw new SemanticException("Class " + ct + " is not accessible.");
        }
        QName currentStatic = currentName;
        ClassType container = (ClassType) resolved;

        // Now try to resolve any remaining elements: inner-class, field, method
        while(stoken.hasMoreTokens()) {
            Name name = Name.make(stoken.nextToken());
            currentStatic = QName.make(currentStatic, name);
            assert (resolved instanceof ClassType);
            // Try to resolve a field or a method
            if (kind() == JL5Import.STATIC_MEMBER) {
                // Try to resolve a field
                FieldInstance fi = null;
                if (((fi=container.fieldNamed(name)) != null) 
                        && ts.isAccessible(fi, ctx) 
                        && fi.flags().isStatic()) {
                    // found valid field
                    return;
                }
                // No luck, try to resolve a method
                List<MethodInstance> meths = container.methodsNamed(name);
                for (MethodInstance mi : meths) { 
                    if (ts.isAccessible(mi, ctx) && mi.flags().isStatic()) {
                        // found valid method
                        return;
                    }
                }
            }
            // Check if name designates an inner-class
            try {
                container = (ClassType) ts.findMemberType(container, name, ctx);
                if (!container.flags().isStatic() && ts.isAccessible(container,ctx)) {
                    throw new SemanticException("Class " + container + " is not accessible.");
                }
            } catch(SemanticException e) {
                // Didn't find a matching name
                throw new SemanticException("The import "+ name + " cannot be resolved");      
            }
        }
        // import has been resolved as a class or an inner-class
        if ((kind() == JL5Import.STATIC_MEMBER)
                && currentStatic.equals(currentName)
                && (!ct.flags().isStatic())) {
            throw new SemanticException("Cannot static import a non-static class");       
        }
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        if (isStaticImport()) {
            checkStaticImport(tc);
            return this;
        }
        return super.typeCheck(tc);
    }

    public String toString() {
        if (isStaticImport()) {
            return "import static " + name + (kind == ALL_STATIC_MEMBERS ? ".*" : "");            
        }
        return super.toString();
    }

    /** Write the import to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        if (isStaticImport()) {
            if (! Globals.Options().fully_qualified_names) {
                w.write("import static ");
                w.write(name.toString());

                if (kind == ALL_STATIC_MEMBERS) {
                    w.write(".*");
                }

                w.write(";");
                w.newline(0);
            }
        } else {
            super.prettyPrint(w, tr);
        }
    }

}
