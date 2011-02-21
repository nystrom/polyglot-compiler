package polyglot.ext.jl5.types;

import polyglot.ast.TypeNode;
import polyglot.main.Report;
import polyglot.types.Context;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeEnv_c;

public class JL5TypeEnv_c extends TypeEnv_c {

	protected JL5TypeSystem jts;
	
	public JL5TypeEnv_c(Context context) {
		super(context);
		this.jts = (JL5TypeSystem) super.ts;
	}

    public boolean isImplicitCastValid(Type fromType, Type toType){
        if (isAutoUnboxingValid(fromType, toType)) {
        	return true;
        }
        
        if (toType instanceof TypeVariable) {
            return isClassToIntersectionValid(fromType, toType);
        }
        //FIXME
        return super.isImplicitCastValid(fromType, toType);
    }
    
    
    //FIXME
    private boolean isClassToIntersectionValid(Type fromType, Type toType){
        TypeVariable it = (TypeVariable)toType;
        if (it.bounds() == null || it.bounds().isEmpty()) return true;
        return isImplicitCastValid(fromType, ((TypeNode)it.bounds().get(0)).type());
    }
    
    public boolean isAutoUnboxingValid(Type fromType, Type toType){
        if (!toType.isPrimitive()) return false;
        if (toType.isInt() && typeEquals(fromType, jts.IntegerWrapper())) return true;
        if (toType.isBoolean() && typeEquals(fromType, jts.BooleanWrapper())) return true;
        if (toType.isByte() && typeEquals(fromType, jts.ByteWrapper())) return true;
        if (toType.isShort() && typeEquals(fromType, jts.ShortWrapper())) return true;
        if (toType.isChar() && typeEquals(fromType, jts.CharacterWrapper())) return true;
        if (toType.isLong() && typeEquals(fromType, jts.LongWrapper())) return true;
        if (toType.isDouble() && typeEquals(fromType, jts.DoubleWrapper())) return true;
        if (toType.isFloat() && typeEquals(fromType, jts.FloatWrapper())) return true;
        return false;
    }
    
	@Override
	public boolean canOverride(MethodInstance mi, MethodInstance mj) {
		return super.canOverride(mi, mj) || super.canOverride(mi, ((JL5MethodInstance)mj).erasure());
	}

	@Override
	public void checkOverride(MethodInstance mi, MethodInstance mj) throws SemanticException {
		// Override to force to always allow covariant return as we are in JL5
		try {
			checkOverride(mi, mj, true); 
		} catch (SemanticException e) {
			checkOverride(mi, ((JL5MethodInstance)mj).erasure(), true);				
		}
	}

	@Override
    public void checkOverride(MethodInstance mii, MethodInstance mjj, boolean allowCovariantReturn) throws SemanticException {

    	JL5MethodInstance mi = (JL5MethodInstance) mii;
    	JL5MethodInstance mj = (JL5MethodInstance) mjj;
    	if (mi == mj)
    	    return;

    	// We override TypeEnv_c's to call the right implementation of hasFormals
    	if (!(mi.name().equals(mj.name()) && mi.hasFormals(mj.formalTypes(), mj.typeVariables(), mj.typeArguments(), context))) {
    	    throw new SemanticException(mi.signature() + " in " + mi.container() + " cannot override " + mj.signature() + " in " + mj.container()
    		    + "; incompatible " + "parameter types", mi.position());
    	}

	    // substitute the type parameters of this method for those of the other method,
	    // so that they can be properly compared to one another
    	mj = (JL5MethodInstance) mj.typeArguments(mi.typeVariables());
	
    	if (allowCovariantReturn ? !isSubtype(mi.returnType(), mj.returnType()) : !typeEquals(mi.returnType(), mj.returnType())) {
    	    if (Report.should_report(Report.types, 3))
    		Report.report(3, "return type " + mi.returnType() + " != " + mj.returnType());
    	    throw new SemanticException(mi.signature() + " in " + mi.container() + " cannot override " + mj.signature() + " in " + mj.container()
    		    + "; attempting to use incompatible " + "return type\n" + "found: " + mi.returnType() + "\n" + "required: " + mj.returnType(),
    					mi.position());
    	}

    	if (!ts.throwsSubset(mi, mj)) {
    	    if (Report.should_report(Report.types, 3))
    		Report.report(3, mi.throwTypes() + " not subset of " + mj.throwTypes());
    	    throw new SemanticException(mi.signature() + " in " + mi.container() + " cannot override " + mj.signature() + " in " + mj.container()
    		    + "; the throw set " + mi.throwTypes() + " is not a subset of the " + "overridden method's throw set " + mj.throwTypes() + ".",
    					mi.position());
    	}

    	if (mi.flags().moreRestrictiveThan(mj.flags())) {
    	    if (Report.should_report(Report.types, 3))
    		Report.report(3, mi.flags() + " more restrictive than " + mj.flags());
    	    throw new SemanticException(mi.signature() + " in " + mi.container() + " cannot override " + mj.signature() + " in " + mj.container()
    		    + "; attempting to assign weaker " + "access privileges", mi.position());
    	}

    	if (mi.flags().isStatic() != mj.flags().isStatic()) {
    	    if (Report.should_report(Report.types, 3))
    		Report.report(3, mi.signature() + " is " + (mi.flags().isStatic() ? "" : "not") + " static but " + mj.signature() + " is "
    			+ (mj.flags().isStatic() ? "" : "not") + " static");
    	    throw new SemanticException(mi.signature() + " in " + mi.container() + " cannot override " + mj.signature() + " in " + mj.container()
    		    + "; overridden method is " + (mj.flags().isStatic() ? "" : "not") + "static", mi.position());
    	}

    	if (!mi.def().equals(mj.def()) && mj.flags().isFinal()) {
    	    // mi can "override" a final method mj if mi and mj are the same
    	    // method instance.
    	    if (Report.should_report(Report.types, 3))
    		Report.report(3, mj.flags() + " final");
    	    throw new SemanticException(mi.signature() + " in " + mi.container() + " cannot override " + mj.signature() + " in " + mj.container()
    		    + "; overridden method is final", mi.position());
    	}
    }
}
