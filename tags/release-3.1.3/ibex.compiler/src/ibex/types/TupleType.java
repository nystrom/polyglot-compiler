package ibex.types;

import java.util.List;

import polyglot.types.ReferenceType;
import polyglot.types.StructType;
import polyglot.types.Type;

public interface TupleType extends ReferenceType, StructType {
    List<Type> elementTypes();
}
