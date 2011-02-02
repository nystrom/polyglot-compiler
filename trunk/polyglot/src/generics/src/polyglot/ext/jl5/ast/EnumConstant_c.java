package polyglot.ext.jl5.ast;

import polyglot.ast.Receiver;
import polyglot.ast.Field_c;
import polyglot.util.Position;

public class EnumConstant_c extends Field_c implements EnumConstant{

    public EnumConstant_c(Position pos, Receiver target, String name){
        super(pos, target, name);
    }
    
    public boolean isConstant(){
        return true;
    }
}
