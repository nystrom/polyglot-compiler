package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.Flags;

public interface JL5ConstructorInstance extends ConstructorInstance, JL5ProcedureInstance{
    JL5ConstructorInstance formalTypes(List ars);
    JL5ConstructorInstance flags(Flags flags);
    JL5ConstructorInstance throwTypes(List l);
    JL5ConstructorInstance container(ClassType container);

}
