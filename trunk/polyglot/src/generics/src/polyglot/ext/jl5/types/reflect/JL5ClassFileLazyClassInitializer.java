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
 * the whoel rawification phase
 * @param ts
 * @return
 * @throws SemanticException
 */
    public ParsedClassType type(TypeSystem ts) throws SemanticException {
    	JL5ParsedClassType t = (JL5ParsedClassType)super.type(ts);
    	if (signature != null) {
    		try {
    			signature.parseClassSignature(ts, t.position());
    		} catch(IOException e){
    		}
    		t.typeVariables(signature.classSignature.typeVars());
    		t.superType(signature.classSignature.superType());
    	}
    	t.superType(((JL5TypeSystem)ts).rawifyBareGenericType(t.superType()));
    	return t;
    }
    
    public void initEnumConstants(JL5ParsedClassType ct){
        JL5TypeSystem ts = (JL5TypeSystem)ct.typeSystem();
        
        for (int i = 0; i < fields.length; i++){
            if ((fields[i].modifiers() & JL5Flags.ENUM_MOD) != 0) {
                FieldInstance fi = fields[i].fieldInstance(ts, ct);
                ct.addEnumConstant(ts.enumInstance(ct.position(), fi.def()));
            }
        }
    }

    public void initAnnotations(JL5ParsedClassType ct){
        JL5TypeSystem ts = (JL5TypeSystem)ct.typeSystem();

        for (int i = 0; i < methods.length; i++){
            MethodInstance mi = methods[i].methodInstance(ts, ct);
            AnnotationElemInstance ai = ts.annotationElemInstance(ct.position(), ct, mi.flags(), mi.returnType(), mi.name(), ((JL5Method)methods[i]).hasDefaultVal());
            ct.addAnnotationElem(ai);
        }
    }
    
      
    /* (non-Javadoc)
     * @see polyglot.types.LazyClassInitializer#initInterfaces(polyglot.types.ParsedClassType)
     */
    public void initInterfaces() {
    	JL5ParsedClassType pct = ct.asType();
        if ((pct instanceof JL5ParsedClassType) && (signature != null)) {
            //JL5ParsedClassType j5ct = (JL5ParsedClassType) ct;
            for (Iterator it = signature.classSignature.interfaces.iterator(); it.hasNext();) {
                Type iface = (Type) it.next();
                ct.addInterface(iface);
            }
        }
        else 
            super.initInterfaces(ct);
    }

	// turn bare occurrences of generic types into raw types
	private static JL5MethodInstance rawifyBareGenerics(JL5MethodDef mi, JL5TypeSystem ts) {
		mi = (JL5MethodInstance) mi.returnType(ts.rawifyBareGenericType(mi.returnType()));
		mi = mi.formalTypes(ts.rawifyBareGenericTypeList(mi.formalTypes()));
		mi = mi.throwTypes(ts.rawifyBareGenericTypeList(mi.throwTypes()));
		//CHECK FIXME:  Should do this as well, but it's causing null pointer exceptions.
		//	mi = (JL5MethodInstance) mi.typeArguments(ts.rawifyBareGenericTypeList(mi.typeArguments()));
		return mi;
	}

	private static JL5ConstructorInstance rawifyBareGenerics(JL5ConstructorInstance ci, JL5TypeSystem ts) {
		ci = ci.formalTypes(ts.rawifyBareGenericTypeList(ci.formalTypes()));
		ci = ci.throwTypes(ts.rawifyBareGenericTypeList(ci.throwTypes()));
		return ci;
	}

    
    
    @Override
    public FieldDef fieldInstance(Field field, ClassDef ct) {
		JL5TypeSystem jts = (JL5TypeSystem) ts;
		FieldDef fd = super.fieldInstance(field, ct);
		// CHECK why do we need to rawify systematically here ?
		FieldInstance fi = fd.asInstance();
		Ref<? extends Type> refType = Types.ref(fi.type());
		Type rawType = jts.rawifyBareGenericType(refType.get());
		if (JL5Flags.isEnumModifier(fi.flags())) {
			fi =  jts.enumInstance(ct.position(), fd);
		}
		return fi;
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
			// CHECK It seems that the signature doesn't contain the thrown exceptions, so we should
			// rely on the super call above to handle that.
			md.setThrowTypes(signature.methodSignature.throwTypes());
		}
		if (md.flags().isTransient()){
			List<Ref<? extends Type>> newFormals = new ArrayList<Ref<? extends Type>>();
			for (Iterator<Ref<? extends Type>> it = md.formalTypes().iterator(); it.hasNext(); ){
				Ref<? extends Type> refType = it.next();
				Type t = refType.get();
				if (!it.hasNext()){
					ArrayType at = (ArrayType) ts.arrayOf(t.position(), ((ArrayType)t).base());
					((JL5ArrayType)at).setVarargs();
					newFormals.add(Types.ref(at));
				} 
				else{
					newFormals.add(Types.ref(t));
				}
			}
			md.setFormalTypes(newFormals);
		}
		return rawifyBareGenerics(md, (JL5TypeSystem) ts);
	}

    protected ConstructorDef constructorInstance(Method method, ClassDef ct, Field[] fields) {
    	ConstructorDef cd = super.constructorInstance(method, ct, fields);
    	JL5Method jl5m = (JL5Method) cd;
    	Signature signature = jl5m.getSignature();
		if (signature != null){
			try {
				signature.parseMethodSignature(this, ts, cd.position(), ct);
			}
			catch(IOException e){
			}
			catch(SemanticException e){
			}
			JL5ConstructorDef jl5cd = (JL5ConstructorDef) cd;
			jl5cd.setTypeVariableTypes(signature.methodSignature.typeVars());
			cd.setFormalTypes(signature.methodSignature.formalTypes());
			// It seems that the signature doesn't contain the thrown exceptions, so we should
			// rely on the super call above to handle that.	    
			// ci = (JL5ConstructorInstance)ci.throwTypes(signature.methodSignature.throwTypes());

		}
		if (cd.flags().isTransient()){
			List<Ref<? extends Type>> newFormals = new ArrayList<Ref<? extends Type>>();
			for (Iterator<Ref<? extends Type>> it = cd.formalTypes().iterator(); it.hasNext(); ){
				Ref<? extends Type> t = it.next();
				if (!it.hasNext()){
					ArrayType at = ((JL5TypeSystem)ts).createArrayType(t.get().position(), ((ArrayType)t).base(), true);
					newFormals.add(Types.ref(at));
				} 
				else{
					newFormals.add(t);
				}
			}
			cd.setFormalTypes(newFormals);
		}
		return rawifyBareGenerics(cd, (JL5TypeSystem) ts);
	}
//	@Override
//	protected boolean initialized() {
//		return super.initialized() && enumConstantInitialized && annotationsInitialized;
//	}

}
