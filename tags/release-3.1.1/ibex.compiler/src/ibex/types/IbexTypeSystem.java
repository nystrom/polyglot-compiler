package ibex.types;

import polyglot.types.ClassType;
import polyglot.types.Name;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;

public interface IbexTypeSystem extends TypeSystem {
    RuleInstance findSymbol(IbexClassType container, Name name) throws SemanticException;

    ClassType runtimeParserType();
    ClassType runtimeByteParserType();
    ClassType runtimeCharParserType();
    ClassType runtimeParserImplType();
    
    Type nullable(Type t);
}