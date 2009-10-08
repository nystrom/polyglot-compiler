package polyglot.bytecode.rep;

import java.util.List;

import polyglot.bytecode.rep.asm.CodeExceptionGen;
import polyglot.bytecode.types.Type;

public interface IMethodGen {
    public void setCode(IOpcodes code);

    public IOpcodes code();

    public int getFlags();

    public void setFlags(int flags);

    public String getName();

    public void setName(String name);

    public Type getReturnType();

    public void setReturnType(Type returnType);

    public List<String> getArgNames();

    public void setArgNames(List<String> argNames);

    public List<Type> getArgTypes();

    public void setArgTypes(List<Type> argTypes);

    public List<Type> getThrowTypes();

    public void setThrowTypes(List<Type> throwTypes);

    public IOpcodes getCode();
}