package polyglot.ext.jl5.types.reflect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import polyglot.ext.jl5.types.AnnotationElemInstance;
import polyglot.ext.jl5.types.JL5ArrayType;
import polyglot.ext.jl5.types.JL5ConstructorDef;
import polyglot.ext.jl5.types.JL5ConstructorInstance;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5MethodDef;
import polyglot.ext.jl5.types.JL5MethodInstance;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.main.Report;
import polyglot.types.ArrayType;
import polyglot.types.ClassDef;
import polyglot.types.ConstructorDef;
import polyglot.types.FieldDef;
import polyglot.types.FieldInstance;
import polyglot.types.MethodDef;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.ClassFileLazyClassInitializer;
import polyglot.types.reflect.Field;
import polyglot.types.reflect.Method;

public class JL5ClassFileLazyClassInitializer extends
		ClassFileLazyClassInitializer {

    protected boolean enumConstantInitialized;
    protected boolean annotationsInitialized;

	public JL5ClassFileLazyClassInitializer(ClassFile file, TypeSystem ts) {
		//CHECK need to implement JL5ClassFileLazyClassInitializer for enum and annotation
		super(file, ts);
	}
	

/**
 * This comes from JL5ClassFile, I think it is now lazily done
 * super return a new ClassDef. Not sure how this relates to 
 * the whole rawification phase
 * @param ts
 * @return
 * @throws SemanticException
 */
//    public ParsedClassType type(TypeSystem ts) throws SemanticException {
//    	JL5ParsedClassType t = (JL5ParsedClassType)super.type(ts);
//    	if (signature != null) {
//    		try {
//    			signature.parseClassSignature(ts, t.position());
//    		} catch(IOException e){
//    		}
//    		t.typeVariables(signature.classSignature.typeVars());
//    		t.superType(signature.classSignature.superType());
//    	}
//    	t.superType(((JL5TypeSystem)ts).rawifyBareGenericType(t.superType()));
//    	return t;
//    }
    
    protected ClassDef createType() throws SemanticException {
    	// The call to super goes over the type creation process
    	// while specializing some of the calls to this class 
    	super.createType();
    	// Here ct has been updated with generics info
    	// Still have to handle enum and annotations
    	initEnumConstants();
    	initAnnotations();
    	
    	return ct;
    }
    
    public void initEnumConstants(){
        // JL5TypeSystem ts = (JL5TypeSystem)ct.typeSystem();
        // If current class represents an enum, then we need 
        // to create an enum constants declaration in the class type.
         
        // CHECK so here we've already initialized fields, but now we would like 
        // to say that was an enum constant declaration.
        // check if we'd rather do that by overridding fieldInstance.
        // => In this case, do we want to introduce an EnumConstantDef that extends FieldDef ?
        
        // OLD CODE from the 
//        for (int i = 0; i < fields.length; i++){
//            if ((fields[i].modifiers() & JL5Flags.ENUM_MOD) != 0) {
//                FieldInstance fi = fields[i].fieldInstance(ts, ct);
//                ct.addEnumConstant(ts.enumInstance(ct.position(), fi.def()));
//            }
//        }
    }
    
    public void initAnnotations(){
    	//CHECK do not handle annotations for now
//    	if (hasSignature()) {
//    		Signature sig = getSignature();
//    		JL5TypeSystem ts = (JL5TypeSystem)ct.typeSystem();
//    		for (int i = 0; i < methods.length; i++){
//    			MethodInstance mi = methods[i].methodInstance(ts, ct);
//    			AnnotationElemInstance ai = ts.annotationElemInstance(ct.position(), ct, mi.flags(), mi.returnType(), mi.name(), ((JL5Method)methods[i]).hasDefaultVal());
//    			ct.addAnnotationElem(ai);
//    		}
//    	}
    }

    protected boolean hasSignature() {
    	return this.getSignature() != null;
    }
    
    protected Signature getSignature() {
    	JL5ClassFile jl5Clazz = (JL5ClassFile) clazz;
    	return jl5Clazz.getSignature();
    }
    
    /* (non-Javadoc)
     * @see polyglot.types.LazyClassInitializer#initInterfaces(polyglot.types.ParsedClassType)
     */
    public void initInterfaces() {
    	JL5ClassFile jl5Clazz = (JL5ClassFile) clazz;
        if (this.hasSignature()) {
        	Signature sig = getSignature();
        	// Need to get a type out of the signature so that it
        	// can be added to the interface list of current class type.
        	// CHECK assume that even if the class doesn't have generics, it contains 
        	// type information that allow us to build interfaces type.
        	for (Iterator<Ref<? extends Type>> it = sig.getSuperInterfacesType().iterator(); it.hasNext();) {
            	Ref<? extends Type> iface = it.next();
                ct.addInterface(iface);
            }
        } else {
        	// Cannot find interface info from the signature (for ex, loading a java 1.4 class)
            super.initInterfaces();
        }
    }

	// CHECK why rawifyBareGenerics ? this code is from the old implementation of JL5Method
    //	// turn bare occurrences of generic types into raw types
//	private static JL5MethodInstance rawifyBareGenerics(JL5MethodDef mi, JL5TypeSystem ts) {
//		mi = (JL5MethodInstance) mi.returnType(ts.rawifyBareGenericType(mi.returnType()));
//		mi = mi.formalTypes(ts.rawifyBareGenericTypeList(mi.formalTypes()));
//		mi = mi.throwTypes(ts.rawifyBareGenericTypeList(mi.throwTypes()));
//		//CHECK FIXME:  Should do this as well, but it's causing null pointer exceptions.
//		//	mi = (JL5MethodInstance) mi.typeArguments(ts.rawifyBareGenericTypeList(mi.typeArguments()));
//		return mi;
//	}
//
//	private static JL5ConstructorInstance rawifyBareGenerics(JL5ConstructorInstance ci, JL5TypeSystem ts) {
//		ci = ci.formalTypes(ts.rawifyBareGenericTypeList(ci.formalTypes()));
//		ci = ci.throwTypes(ts.rawifyBareGenericTypeList(ci.throwTypes()));
//		return ci;
//	}
    
    @Override
    public FieldDef fieldInstance(Field field, ClassDef ct) {
		JL5TypeSystem jts = (JL5TypeSystem) ts;
		FieldDef fd = super.fieldInstance(field, ct);
		// CHECK why rawifyBareGenerics ?
//		FieldInstance fi = fd.asInstance();
//		Ref<? extends Type> refType = Types.ref(fi.type());
//		Type rawType = jts.rawifyBareGenericType(refType.get());
//		if (JL5Flags.isEnumModifier(fi.flags())) {
//			fi =  jts.enumInstance(ct.position(), fd);
//		}
		return fd;
	}

    protected MethodDef methodInstance(Method method, ClassDef ct) {
    	MethodDef md = super.methodInstance(method, ct);
    	JL5Method jl5m = (JL5Method) method;
    	Signature signature = jl5m.getSignature();
		if (signature != null){
			try {
				signature.parseMethodSignature(this, ts, md.position(), ct);
			}
			catch(IOException e){
			}
			JL5MethodDef jl5Md = (JL5MethodDef) md;
			// here we must get reference on TV
			jl5Md.setTypeVariableTypes(signature.methodSignature.typeVars());
			md.setReturnType(signature.methodSignature.returnType());
			md.setFormalTypes(signature.methodSignature.formalTypes());
			// CHECK It seems that the signature doesn't contain thrown exceptions,
			// so we should rely on the super call above to handle that.
			md.setThrowTypes(signature.methodSignature.throwTypes());
		}
		//CHECK why do we need to do that if method is transient ?
		// If a method is transient we look for the last argument and we replace its
		// type by an arraytype and set the varargs on the array
		if (md.flags().isTransient()){
			assert false;
			List<Ref<? extends Type>> newFormals = new ArrayList<Ref<? extends Type>>();
			for (Iterator<Ref<? extends Type>> it = md.formalTypes().iterator(); it.hasNext(); ){
				Ref<? extends Type> refType = it.next();
				Type t = refType.get();
				if (!it.hasNext()){
					ArrayType at = (ArrayType) ((JL5TypeSystem) ts).createArrayType(t.position(), Types.ref(((ArrayType)t).base()), true);
					newFormals.add(Types.ref(at));
				} 
				else{
					newFormals.add(Types.ref(t));
				}
			}
			md.setFormalTypes(newFormals);
		}
		return md;
		// CHECK why rawifyBareGenerics ?
//		return rawifyBareGenerics(md, (JL5TypeSystem) ts);
	}

    protected ConstructorDef constructorInstance(Method method, ClassDef ct, Field[] fields) {
    	ConstructorDef cd = super.constructorInstance(method, ct, fields);
    	JL5Method jl5m = (JL5Method) method;
    	Signature signature = jl5m.getSignature();
		if (signature != null){
			try {
				signature.parseMethodSignature(this, ts, cd.position(), ct);
			}
			catch(IOException e){
			}
			JL5ConstructorDef jl5cd = (JL5ConstructorDef) cd;
			jl5cd.setTypeVariableTypes(signature.methodSignature.typeVars());
			cd.setFormalTypes(signature.methodSignature.formalTypes());
			// It seems that the signature doesn't contain the thrown exceptions, so we should
			// rely on the super call above to handle that.	    
			// ci = (JL5ConstructorInstance)ci.throwTypes(signature.methodSignature.throwTypes());

		}
		//CHECK why do we need to do that if method is transient ?
		// If a method is transient we look for the last argument and we replace its
		// type by an arraytype and set the varargs on the array
		if (cd.flags().isTransient()){
			assert false;
			List<Ref<? extends Type>> newFormals = new ArrayList<Ref<? extends Type>>();
			for (Iterator<Ref<? extends Type>> it = cd.formalTypes().iterator(); it.hasNext(); ){
				Ref<? extends Type> t = it.next();
				if (!it.hasNext()){
					ArrayType at = ((JL5TypeSystem)ts).createArrayType(t.get().position(), Types.ref(((ArrayType)t).base()), true);
					newFormals.add(Types.ref(at));
				} else {
					newFormals.add(t);
				}
			}
			cd.setFormalTypes(newFormals);
		}
		// CHECK why rawifyBareGenerics ?
//		return rawifyBareGenerics(cd, (JL5TypeSystem) ts);
		return cd;
	}
//	@Override
//	protected boolean initialized() {
//		return super.initialized() && enumConstantInitialized && annotationsInitialized;
//	}

}
