package polyglot.ext.jl5.ast;

import polyglot.ast.Field_c;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Receiver;
import polyglot.ext.jl5.types.EnumInstance;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.ParameterizedType;
import polyglot.types.Context;
import polyglot.types.FieldInstance;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.ContextVisitor;

public class JL5Field_c extends Field_c implements JL5Field {

    public JL5Field_c (Position pos, Receiver target, Id name){
        super(pos, target, name);
    }

    public Node typeCheck(ContextVisitor tc) throws SemanticException {
    	Context c = tc.context();
    	JL5TypeSystem ts = (JL5TypeSystem) tc.typeSystem();

    	FieldInstance fi = ts.findFieldOrEnum(target.type(), ts.FieldMatcher(target.type(), name.id(), c));


    	if (fi == null) {
    		throw new InternalCompilerError("Cannot access field on node of type " +
    				target.getClass().getName() + ".");
    	}

    	JL5Field_c f = (JL5Field_c) fieldInstance(fi).type(fi.type());  
    	f.checkConsistency(c);

    	if (target().type() instanceof ParameterizedType && 
    			fi.type() instanceof ParameterizedType){
    		if (ts.typeEquals(((ParameterizedType)fi.type()).baseType(), 
    						((ParameterizedType)target.type()).baseType(),
    						tc.context())){
    			//CHECK why do we need to change the type of 'f' to the one of the Receiver ?
    			return f.type((ParameterizedType)target().type());
    		}
    	}
    	return f;
    }

    public boolean isConstant(){
    	//CHECK isn't it enough to check for the enum modifier flag ?
        if (JL5Flags.isEnumModifier(flags())) return true;
        if (fieldInstance() instanceof EnumInstance) return true;
        return super.isConstant();
    }
    
    @Override
    public void checkConsistency(Context c){
        //this consistency checking has problems when dealing with gen types        
        super.checkConsistency(c);
    }
}
