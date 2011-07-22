package polyglot.ext.jl5.types;

import polyglot.ast.TypeNode;
import polyglot.ext.jl5.types.inference.LubType;
import polyglot.main.Report;
import polyglot.types.Context;
import polyglot.types.MethodInstance;
import polyglot.types.PrimitiveType;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeEnv_c;

public class JL5TypeEnv_c extends TypeEnv_c {

	protected JL5TypeSystem jts;
	
	public JL5TypeEnv_c(Context context) {
		super(context);
		this.jts = (JL5TypeSystem) super.ts;
	}

    
	public boolean isImplicitCastValid(Type fromType, Type toType) {

		if (fromType instanceof IntersectionType) {
			IntersectionType it = (IntersectionType) fromType;
	        for (Type b : it.boundsTypes()) {
	            if (isImplicitCastValid(b, toType)) {
	            	return true;
	            }
	        }
	        return false;
		}
		
		if (fromType instanceof LubType) {
			for (Type elem : ((LubType) fromType).lubElements()) {
				if (!isImplicitCastValid(elem, toType)) return false;
			}
			return true;
		}
		
		if (toType instanceof TypeVariable) {
			TypeVariable tv = (TypeVariable) toType;
			return super.isImplicitCastValid(fromType, tv.lowerBound())
			|| super.isImplicitCastValid(fromType, toType);
		}
		//from ClassType to TypeVariable
//		if (toType instanceof TypeVariable){
//			return isClassToIntersectionValid(fromType, toType);
//		}

		// boxing
		if ((toType instanceof ReferenceType) && 
				isPrimitiveNonVoid(fromType)) {
			return isImplicitCastValid(jts.classOf(fromType),toType);
		}

		// unboxing
		if ((fromType instanceof ReferenceType) && 
				isPrimitiveNonVoid(toType)) {
			return isImplicitCastValid(fromType, jts.classOf(toType));
		} 

		if ((fromType instanceof ParameterizedType) && 
				(toType instanceof JL5ParsedClassType)) {
			return super.isImplicitCastValid(jts.erasure(fromType), toType);
		}
		// from Ref to Ref or only Primitive and Arrays involved
		return super.isImplicitCastValid(fromType, toType);
	}
	
	private boolean isPrimitiveNonVoid(Type t) {
		return t.isPrimitive() && !t.isVoid();
	}
    
	/**
	 * @deprecated
	 * @param fromType
	 * @param toType
	 * @return
	 */
    private boolean isClassToIntersectionValid(Type fromType, Type toType){
        TypeVariable it = (TypeVariable)toType;
        if (it.bounds() == null || it.bounds().isEmpty()) return true;
        return isImplicitCastValid(fromType, ((TypeNode)it.bounds().get(0)).type());
    }

    /**
     * Returns true if <code>value</code> can be implicitly cast to
     * Primitive or Box type <code>t</code>.
     */
    @Override
    public boolean numericConversionValid(Type t, Object value) {
    	if (t instanceof PrimitiveType) {
    		return super.numericConversionValid(t, value);
    	} else {
    		// boxing / unboxing
            if (value == null) return false;

        	if (value instanceof Float || value instanceof Double)
        	    return false;

            long v;
            if (value instanceof Number){
                v = ((Number) value).longValue();
            }
            else if (value instanceof Character){
                v = ((Character) value).charValue();
            }
            else {
                return false;
            }

            if (typeEquals(t, jts.LongWrapper()) && value instanceof Long) return true;
            if (typeEquals(t, jts.IntegerWrapper()) && value instanceof Integer) return Integer.MIN_VALUE <= v && v <= Integer.MAX_VALUE;
            if (typeEquals(t, jts.CharacterWrapper()) && value instanceof Character) return Character.MIN_VALUE <= v && v <= Character.MAX_VALUE;
            if (typeEquals(t, jts.ShortWrapper()) && value instanceof Short) return Short.MIN_VALUE <= v && v <= Short.MAX_VALUE;
            if (typeEquals(t, jts.ByteWrapper()) && value instanceof Byte) return Byte.MIN_VALUE <= v && v <= Byte.MAX_VALUE;

            return false;
    	}
    }
    
    @Override
    public boolean isSubtype(Type t1, Type t2) {
    	// does t1 is a subtype of t2 ?
    	// If t1 is a 
    	return super.isSubtype(t1, t2);
    }
    
    /**
     * Take into account boxing and unboxing cast
     */
    @Override
    public boolean isCastValid(Type fromType, Type toType) {
    	
		if (fromType instanceof IntersectionType) {
			IntersectionType it = (IntersectionType) fromType;
	        for (Type b : it.boundsTypes()) {
	            if (isCastValid(b, toType)) {
	            	return true;
	            }
	        }
	        return false;
		}

		if(fromType instanceof LubType) {
			for (Type elem : ((LubType)fromType).lubElements()) {
				if (!isCastValid(elem, toType)) return false;
			}
			return true;
		}
		
    	if (fromType instanceof TypeVariable) {
    		return isCastValid(((TypeVariable) fromType).upperBound(), toType);
    	}
    	
    	// boxing / unboxing cast check
    	if (fromType.isPrimitive() && toType.isReference()) {
    		return equivalent(fromType, toType);
    	}
    	
    	if (fromType.isReference() && toType.isPrimitive()) {
    		// check if fromType is a boxed type
    		PrimitiveType pt = fromType.toPrimitive();
    		
    		// if fromType is a box, as toPrimitive returns != null
    		// then we use primitive to primitive cast conversion rules
    		if (pt != null) {
    			return super.isCastValid(pt, toType);
    		}
    	}

    	// regular casts
    	return super.isCastValid(fromType, toType);
    }

    
    public boolean equivalent(Type fromType, Type toType) {
        if (fromType instanceof GenericTypeRef)
            return ((GenericTypeRef) fromType).equivalentImpl(toType);
        if (fromType instanceof TypeVariable)
            return ((TypeVariable) fromType).equivalentImpl(toType);
        
    	if ((fromType instanceof JL5PrimitiveType) || (fromType instanceof JL5ParsedClassType)) {
	        if (fromType.isBoolean() && toType.isBoolean()) return true;
	        if (fromType.isInt() && toType.isInt()) return true;
	        if (fromType.isByte() && toType.isByte()) return true;
	        if (fromType.isShort() && toType.isShort()) return true;
	        if (fromType.isChar() && toType.isChar()) return true;
	        if (fromType.isLong() && toType.isLong()) return true;
	        if (fromType.isFloat() && toType.isFloat()) return true;
	        if (fromType.isDouble() && toType.isDouble()) return true;
    	}
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
