package polyglot.ext.jl5.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import polyglot.types.ClassDef;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.Name;
import polyglot.types.Named;
import polyglot.types.ParsedClassType_c;
import polyglot.types.PrimitiveType;
import polyglot.types.Ref;
import polyglot.types.Resolver;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.types.Types;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.TypedList;

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
            assert(false);
            // CHECK: lazy init has been erased because it wasn't doing anything 
            // ((JL5LazyClassInitializer)init).initEnumConstants(this);
            // freeInit();
        }
        return enumConstants;
    }

    public List<AnnotationElemInstance> annotationElems(){
        if (annotationElems == null){
            annotationElems = new TypedList(new LinkedList(), AnnotationElemInstance.class, false);
            assert(false);
            // CHECK: lazy init has been erased because it wasn't doing anything 
            // ((JL5LazyClassInitializer)init).initAnnotations(this);
            // freeInit();
        }
        return annotationElems;
    }

    protected boolean initialized(){
        return super.initialized() && this.enumConstants != null && this.annotationElems != null;
    }
    
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

    public void addMethod(MethodInstance mi){
        if (JL5Flags.isAnnotationModifier(flags())){
            //addAnnotationElem(ts.annotationElemInstance(mi.position(), this, mi.flags(), mi.type(), mi,name(), 
                        
        }
        super.addMethod(mi);
    }

    /**
     * find methods with compatible name and formals as the given one
     */
    public List<MethodInstance> methods(JL5MethodInstance mi) {
        List l = new LinkedList();
        List<JL5ProcedureInstance> methods = (List) methodsNamed(mi.name());
        for (JL5ProcedureInstance pi : methods) {
            if (pi.hasFormals(mi)) {
                l.add(pi);
            }
        }
	return l;
    }

    public List<TypeVariable> typeVariables(){
        return typeVariables;
    }
    
    // this is used when coming from source files
    public void addTypeVariable(TypeVariable type){
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
    
    public boolean hasTypeVariable(String name){
        if (typeVariables == null || typeVariables.isEmpty()) return false;
        for (Iterator<TypeVariable> it = typeVariables.iterator(); it.hasNext(); ){
            TypeVariable iType = it.next();
            if (iType.name().equals(name)) return true;
        }
        return false;
    }

    public TypeVariable getTypeVariable(String name){
        for (Iterator<TypeVariable> it = typeVariables.iterator(); it.hasNext(); ){
            TypeVariable iType = it.next();
            if (iType.name().equals(name)) return iType;
        }
        return null;
    }

    public boolean isGeneric(){
        if ((typeVariables != null) && !typeVariables.isEmpty()) return true;
        return false;
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
        return super.toPrimitive();
    }
    
    public String translate(Resolver c) {
        if (isTopLevel()) {
            if (package_() == null) {
                return name();
            }

            // Use the short name if it is unique.
            if (c != null) {
                try {
                    Named x = c.find(name());

                    if (ts.equals(this, x)) {
                        return name();
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
                return name();
            }

            // Use the short name if it is unique.
            if (c != null) {
                try {
                    Named x = c.find(name());

                    if (ts.equals(this, x) && !(container() instanceof ParameterizedType)) {
                        return name();
                    }
                }
                catch (SemanticException e) {
                }
            }

            return container().translate(c) + "." + name();
        }
        else if (isLocal()) {
            return name();
        }
        else {
            throw new InternalCompilerError("Cannot translate an anonymous class.");
        }
    }

    public String signature(){
        return "L"+fullName().replaceAll("\\.", "/")+";";
    }
}
