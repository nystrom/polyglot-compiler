/**
 * 
 */
package polyglot.bytecode.rep.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import polyglot.bytecode.rep.IMethodGen;
import polyglot.bytecode.rep.IOpcodes;
import polyglot.bytecode.types.Type;

public class MethodGen implements IMethodGen {
    public MethodGen(int flags, String name, Type returnType, Type[] argTypes, String[] argNames, Type[] throwTypes) {
        this.flags = flags;
        this.returnType = returnType;
        this.argTypes = Arrays.asList(argTypes);
        this.argNames = Arrays.asList(argNames);
        this.name = name;
        this.throwTypes = Arrays.asList(throwTypes);
        this.code = null;
    }
    
    MethodNode mn() {
        String[] throwTypeStrings = new String[throwTypes.size()];
        for (int i = 0; i < throwTypes.size(); i++)
            throwTypeStrings[i] = throwTypes.get(i).desc();
        MethodNode mn = new MethodNode(flags, name, Bytecodes.methodSignature(argTypes.toArray(new Type[0]), returnType), null, throwTypeStrings);
        mn.instructions = code.instructions;
        mn.tryCatchBlocks = code.tryCatchBlocks;
        return mn;
    }

    int flags;
    String name;
    Type returnType;
    List<String> argNames;
    List<Type> argTypes;
    List<Type> throwTypes;
    Bytecodes code;
    
    public void setCode(IOpcodes code) {
        this.code = (Bytecodes) code;
    }
    
    public IOpcodes code() {
        return code;
    }
    
    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }

    public List<String> getArgNames() {
        return argNames;
    }

    public void setArgNames(List<String> argNames) {
        this.argNames = argNames;
    }

    public List<Type> getArgTypes() {
        return argTypes;
    }

    public void setArgTypes(List<Type> argTypes) {
        this.argTypes = argTypes;
    }

    public List<Type> getThrowTypes() {
        return throwTypes;
    }

    public void setThrowTypes(List<Type> throwTypes) {
        this.throwTypes = throwTypes;
    }

    public IOpcodes getCode() {
        return code;
    }
}