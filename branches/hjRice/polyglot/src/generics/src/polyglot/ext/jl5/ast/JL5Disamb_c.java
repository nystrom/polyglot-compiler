package polyglot.ext.jl5.ast;

import polyglot.ast.Disamb_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.JL5Context;
import polyglot.ext.jl5.types.JL5NoMemberException;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.FieldInstance;
import polyglot.types.Named;
import polyglot.types.NoClassException;
import polyglot.types.NoMemberException;
import polyglot.types.Package;
import polyglot.types.QName;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.Types;
import polyglot.types.VarInstance;

public class JL5Disamb_c extends Disamb_c {

        
	/**
	 * We look both for fields and enums in JL5
	 */
	@Override
    protected Node disambiguateTypeNodePrefix(TypeNode tn) throws SemanticException {
        Type t = tn.type();
   
        if (exprOK()){
        	// IN JL5 Search for both Fields and Enum
            try {
            	//CHECK we can refactor upper class to isolate this change
                FieldInstance fi = ((JL5TypeSystem)ts).findFieldOrEnum(t, ts.FieldMatcher(t, name.id(), c));
                return nf.Field(pos, tn, name).fieldInstance(fi);
            }
            catch(NoMemberException e){
                if (e.getKind() != NoMemberException.FIELD && e.getKind() != JL5NoMemberException.ENUM_CONSTANT){
                    // something went wrong...
                	throw e;
                }
            }
        }

        // Try member classes.
        if (t.isClass() && typeOK()) {
            Resolver tc = t.toClass().resolver();
            Named n;
            try {
                n = tc.find(ts.MemberTypeMatcher(t, name.id(), c));
            }
            catch (NoClassException e) {
                return null;
            }
            if (n instanceof Type) {
                Type type = (Type) n;
                return makeTypeNode(type);
            }
        }

        return null;
    }

	@Override
    protected Node disambiguateNoPrefix() throws SemanticException {

		/* begin Copy-Paste from super */
		if (exprOK()) {
            // First try local variables and fields.
            VarInstance vi = c.findVariableSilent(name.id());
            
            if (vi != null) {
                Node n = disambiguateVarInstance(vi);
                if (n != null) return n;
            }
        }
        /* end Copy-Paste from super */

        //if (((JL5Context)c).inTypeVariable()){
        TypeVariable res = ((JL5Context)c).findTypeVariableInThisScope(name.id());
        if (res != null){
            return nf.CanonicalTypeNode(pos, res);
        }
        //}
        
        // no variable found. try
        // might be a generic type parameter
        /*JL5ParsedClassType ct = (JL5ParsedClassType)c.currentClass();
    
        
        if ((ct != null ) && ct.isGeneric()){
            if (ct.hasTypeVariable(name)){
          /*      TypeVariable it =  ct.getTypeVariable(name);
                return nf.CanonicalTypeNode(pos, it);
            }
        }

        // may be a generic type param for method or constr
        // header 
        if (c.inCode()){
            CodeInstance ci = c.currentCode();
            if (ci instanceof JL5MethodInstance && ((JL5MethodInstance)ci).isGeneric()){
                if (((JL5MethodInstance)ci).hasTypeVariable(name)){
                    return nf.CanonicalTypeNode(pos, ((JL5MethodInstance)ci).getTypeVariable(name));
                }
            }
            else if (ci instanceof JL5ConstructorInstance && ((JL5ConstructorInstance)ci).isGeneric()){
                if (((JL5ConstructorInstance)ci).hasTypeVariable(name)){
                    return nf.CanonicalTypeNode(pos, ((JL5ConstructorInstance)ci).getTypeVariable(name));
                }
            }
        }*/

        /* begin Copy-Paste from super */
        // no variable found. try types.
        if (typeOK()) {
            try {
                Named n = c.find(ts.TypeMatcher(name.id()));
                if (n instanceof Type) {
                    Type type = (Type) n;
                    return makeTypeNode(type);
                }
            } catch (NoClassException e) {
                if (!name.id().toString().equals(e.getClassName())) {
                    // hmm, something else must have gone wrong
                    // rethrow the exception
                    throw e;
                }

                // couldn't find a type named name. 
                // It must be a package--ignore the exception.
            }
        }

        // Must be a package then...
        if (packageOK()) {
            try {
        	Package p = ts.packageForName(QName.make(null, name.id()));
        	return nf.PackageNode(pos, Types.ref(p));
            }
            catch (SemanticException e) {
            }
            Package p = ts.createPackage(QName.make(null, name.id()));
            return nf.PackageNode(pos, Types.ref(p));
        }

        return null;
        /* end Copy-Paste from super */ 
        
        //Node result = null;
        //if (result == null){
            // make special AmbNoPrefix node and return it (it should 
            // extend Expr)
            // later have a pass (after type checking) to deal with these
            // nodes which may be an enum constant of the type of a 
            // switch expr (this situation may arise for case labels)
            // otherwise as far as I know its an error
                
          //  result = ((JL5NodeFactory)nf).JL5AmbExpr(pos, name);
        //}
        //return result;
    }
}
