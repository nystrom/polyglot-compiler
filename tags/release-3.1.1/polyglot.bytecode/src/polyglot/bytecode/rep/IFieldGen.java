package polyglot.bytecode.rep;

import polyglot.bytecode.types.Type;

public interface IFieldGen {

    int getFlags();

    void setFlags(int flags);

    String getName();

    void setName(String name);

    Type getType();

    void setType(Type type);

}