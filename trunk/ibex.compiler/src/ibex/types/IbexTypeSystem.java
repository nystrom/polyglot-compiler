package ibex.types;

import java.util.List;

import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.Name;
import polyglot.types.Ref;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.Position;

public interface IbexTypeSystem extends TypeSystem {
    RuleInstance findSymbol(IbexClassType container, Name name) throws SemanticException;

    ClassType runtimeParserType();
    ClassType runtimeByteParserType();
    ClassType runtimeCharParserType();
    ClassType runtimeParserImplType();
    
    Type nullable(Type t);
    
    RuleDef ruleDef(Position pos, Ref<? extends ClassType> container,
            Flags flags, Ref<? extends Type> type, Name name, List<Ref<? extends Type>> throwTypes);
}
