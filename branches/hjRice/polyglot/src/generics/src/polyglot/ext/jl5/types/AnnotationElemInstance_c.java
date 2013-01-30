package polyglot.ext.jl5.types;

import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.MemberInstance;
import polyglot.types.Name;
import polyglot.types.StructType;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeObject_c;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public class AnnotationElemInstance_c extends TypeObject_c implements AnnotationElemInstance {
    
    protected Flags flags;
    protected Type type;
    protected Name name;
    protected ClassType container;
    protected boolean hasDefault;
        
    public AnnotationElemInstance_c(TypeSystem ts, Position pos, ClassType ct, Flags flags, Type type, Name name, boolean hasDef){
        super(ts, pos);
        this.flags = flags;
        this.type = type;
        this.name = name;
        this.container = ct;
        this.hasDefault = hasDef;
        throw new RuntimeException("Annotation elements declaration are not yet supported");
    }

    public Flags flags(){
        return flags;
    }

    public Type type(){
        return type;
    }

    public Name name(){
        return name;
    }

    public boolean isCanonical(){
        return true;
    }

    public boolean hasDefault(){
        return hasDefault;
    }

	@Override
	public MemberInstance flags(Flags flags) {
		//  CHECK AnnotationElemDecl comment code to be able to compile the generic extension
		return null;
	}

	@Override
	public StructType container() {
		//  CHECK AnnotationElemDecl comment code to be able to compile the generic extension
		return null;
	}

	@Override
	public MemberInstance container(StructType t) {
		//  CHECK AnnotationElemDecl comment code to be able to compile the generic extension
		return null;
	}

	@Override
	public TypeObject def() {
		//  CHECK AnnotationElemDecl comment code to be able to compile the generic extension
		return null;
	}
}
