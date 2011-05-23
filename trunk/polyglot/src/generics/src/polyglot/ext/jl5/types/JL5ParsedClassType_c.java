package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.frontend.Globals;
import polyglot.types.ClassDef;
import polyglot.types.ClassType;
import polyglot.types.DerefTransform;
import polyglot.types.Name;
import polyglot.types.Named;
import polyglot.types.ParsedClassType_c;
import polyglot.types.PrimitiveType;
import polyglot.types.Ref;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.TransformingList;
import polyglot.util.TypedList;

/**
 * A JL5 parsed class. Has supports for:
 * - Annotations
 * - Can be used as an enum type: (enumConstants are added later on) 
 */
public class JL5ParsedClassType_c extends ParsedClassType_c implements JL5ParsedClassType, SignatureType{
    protected List<EnumInstance> enumConstants;
    // these are annotation elements in the annotation type
    protected List<AnnotationElemInstance> annotationElems;
  
    // these are annotations that have been declared on (applied to) the type
    protected List annotations;
    protected List<TypeVariable> typeVariables;

    public JL5ParsedClassType_c(ClassDef def) {
        this(def.typeSystem(), def.position(), Types.ref(def));
    }

    public JL5ParsedClassType_c(TypeSystem ts, Position pos, Ref<? extends ClassDef> def) {
        super(ts, pos, def);
    }
    
    public void annotations(List annotations){
        this.annotations = annotations;
    }

    public List annotations(){
        return annotations;
    }
        
    
    public void addEnumConstant(EnumInstance ei){
        enumConstants().add(ei);
    }

    public void addAnnotationElem(AnnotationElemInstance ai){
        annotationElems().add(ai);
    }

    public List<EnumInstance> enumConstants(){
        if (enumConstants == null){
            enumConstants = new TypedList(new LinkedList(), EnumInstance.class, false);
            // CHECK: lazy init has been erased because it wasn't doing anything 
        }
        return enumConstants;
    }

    public List<AnnotationElemInstance> annotationElems(){
        if (annotationElems == null){
            annotationElems = new TypedList(new LinkedList(), AnnotationElemInstance.class, false);
            // CHECK: lazy init has been erased because it wasn't doing anything 
            // ((JL5LazyClassInitializer)init).initAnnotations(this);
            // freeInit();
        }
        return annotationElems;
    }

//    protected boolean initialized(){
//        return super.initialized() && this.enumConstants != null && this.annotationElems != null;
//    }
    
    public EnumInstance enumConstantNamed(Name name) {
        for (Iterator<EnumInstance> i = enumConstants().iterator(); i.hasNext(); ) {
        	EnumInstance fi = i.next();
            if (fi.name().equals(name)) {
                return fi;
            }
        }
        return null;
    }
    
    public AnnotationElemInstance annotationElemNamed(Name name){
        for(Iterator<AnnotationElemInstance> it = annotationElems().iterator(); it.hasNext();){
            AnnotationElemInstance ai = it.next();
            if (ai.name().equals(name)){
                return ai;
            }
        }
        return null;
    }

//    public void addMethod(MethodInstance mi){
//        if (JL5Flags.isAnnotationModifier(flags())){
//            //addAnnotationElem(ts.annotationElemInstance(mi.position(), this, mi.flags(), mi.type(), mi,name(), 
//        }
//        super.addMethod(mi);
//    }
//
//    /**
//     * find methods with compatible name and formals as the given one
//     */
//    public List<MethodInstance> methods(JL5MethodInstance mi) {
//        List l = new LinkedList();
//        List<JL5ProcedureInstance> methods = (List) methodsNamed(mi.name());
//        for (JL5ProcedureInstance pi : methods) {
//            if (pi.hasFormals(mi)) {
//                l.add(pi);
//            }
//        }
//	return l;
//    }

    public List<TypeVariable> typeVariables() {
        List l = new TransformingList<Ref<? extends Type>,Type>(((JL5ClassDef)def()).typeVariables(),
                new DerefTransform<Type>());
        return l;
    }
    
    // this is used when coming from source files
    public void addTypeVariable(TypeVariable type) {
        if (typeVariables == null){
            typeVariables = new ArrayList<TypeVariable>();
        }
        if (!typeVariables.contains(type)){
            typeVariables.add(type);
            type.declaringClass(this);
        }
    }

    // this is used when coming from class files
    public void typeVariables(List<TypeVariable> vars){
        typeVariables = vars;
        for (TypeVariable variable : vars) {
            variable.declaringClass(this);
        }
    }
    
    public boolean hasTypeVariable(Name name){
        if (typeVariables == null || typeVariables.isEmpty()) return false;
        for (Iterator<TypeVariable> it = typeVariables.iterator(); it.hasNext(); ){
            TypeVariable iType = it.next();
            if (iType.name().equals(name)) return true;
        }
        return false;
    }

    public TypeVariable getTypeVariable(Name name){
        for (Iterator<TypeVariable> it = typeVariables.iterator(); it.hasNext(); ){
            TypeVariable iType = it.next();
            if (iType.name().equals(name)) return iType;
        }
        return null;
    }

    public boolean isGeneric(){
        return ((typeVariables != null) && !typeVariables.isEmpty());
    }

    // this is only for debugging or something
    public String toString(){
        StringBuffer sb = new StringBuffer(super.toString());
        if ((typeVariables != null) && !typeVariables.isEmpty()){
            sb.append("<");
            sb.append(typeVariables);
            sb.append(">");
        }
        return sb.toString();
    }

    /**
     * If class is a box type, returns its primitive counterpart
     */
    @Override
    public PrimitiveType toPrimitive() {
        TypeSystem ts = typeSystem();
        if (ts.isBoolean(this)) return (PrimitiveType) ts.Boolean();
        if (ts.isByte(this)) return (PrimitiveType) ts.Byte();
        if (ts.isShort(this)) return (PrimitiveType) ts.Short();
        if (ts.isChar(this)) return (PrimitiveType) ts.Char();
        if (ts.isInt(this)) return (PrimitiveType) ts.Int();
        if (ts.isLong(this)) return (PrimitiveType) ts.Long();
        if (ts.isFloat(this)) return (PrimitiveType) ts.Float();
        if (ts.isDouble(this)) return (PrimitiveType) ts.Double();
        if (ts.isVoid(this)) return (PrimitiveType) ts.Void();
        return super.toPrimitive();
    }
    
    /**
     * Copy-paste from ClassType_C
     */
    @Override
    public String translate(Resolver c) {
    	// CHECK if we really need to copy-paste this.
        if (isTopLevel()) {
            if (package_() == null) {
                return name().toString();
            }
            // Use the short name if it is unique.
            if (c != null && !Globals.Options().fully_qualified_names) {
                try {
                    Named x = c.find(ts.TypeMatcher(name()));
                    
                    if (x instanceof ClassType && def().equals(((ClassType) x).def())) {
                        return name().toString();
                    }
                }
                catch (SemanticException e) {
                }
            }
            return package_().translate(c) + "." + name();
        }
        else if (isMember()) {
            // Use only the short name if the outer class is anonymous.
            if (container().toClass().isAnonymous()) {
                return name().toString();
            }
            // Use the short name if it is unique.
            if (c != null && !Globals.Options().fully_qualified_names) {
                try {
                    Named x = c.find(ts.TypeMatcher(name()));
                    // Modification wrt to copy-paste code here:
                    if (x instanceof ClassType && def().equals(((ClassType) x).def())
                    		&& !(container() instanceof ParameterizedType)) {
                        return name().toString();
                    }
                }
                catch (SemanticException e) {
                }
            }
            return container().translate(c) + "." + name();
        }
        else if (isMember()) {
            // Use only the short name if the outer class is anonymous.
            if (container().toClass().isAnonymous()) {
                return name().toString();
            }

            // Use the short name if it is unique.
            if (c != null && !Globals.Options().fully_qualified_names) {
                try {
                    Named x = c.find(ts.TypeMatcher(name()));

                    if (x instanceof ClassType && def().equals(((ClassType) x).def())) {
                        return name().toString();
                    }
                }
                catch (SemanticException e) {
                }
            }

            return container().translate(c) + "." + name();
        }
        else if (isLocal()) {
            return name().toString();
        }
        else {
            throw new InternalCompilerError("Cannot translate an anonymous class.");
        }
    }

    public String signature(){
        return "L"+fullName().toString().replaceAll("\\.", "/")+";";
    }

	@Override
	public boolean equivalentImpl(TypeObject arg2) {
		return equalsImpl(arg2);
	}
}
