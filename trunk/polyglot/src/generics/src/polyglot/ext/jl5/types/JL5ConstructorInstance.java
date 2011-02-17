package polyglot.ext.jl5.types;

import java.util.List;

import polyglot.types.ConstructorDef;
import polyglot.types.ConstructorInstance;
import polyglot.types.Flags;
import polyglot.types.StructType;
import polyglot.types.Type;

public interface JL5ConstructorInstance extends JL5ProcedureInstance<ConstructorDef>, ConstructorInstance {
    JL5ConstructorInstance flags(Flags flags);
    JL5ConstructorInstance formalTypes(List<Type> ars);
    JL5ConstructorInstance throwTypes(List<Type> l);
    JL5ConstructorInstance container(StructType container);

}
