package polyglot.bytecode.rep;

import polyglot.bytecode.rep.asm.Bytecodes;
import polyglot.bytecode.rep.asm.ClassGen;
import polyglot.bytecode.rep.asm.FieldGen;
import polyglot.bytecode.rep.asm.MethodGen;
import polyglot.bytecode.types.Empty;
import polyglot.bytecode.types.StackType;
import polyglot.bytecode.types.Type;

public class AsmFactory {

    public static ClassGen makeClass(String name, int flags, Type superClass, Type[] interfaces, String fileName) {
        return new ClassGen(name, superClass, fileName, flags, interfaces);
    }

    public static IOpcodes makeOpcodes(IMethodGen mg, StackType st) {
        Bytecodes bc = new Bytecodes((MethodGen) mg, st);
        mg.setCode(bc);
        return bc;

    }
    public static IOpcodes makeOpcodes(IMethodGen mg) {
        return makeOpcodes(mg, Empty.it);
    }

    public static FieldGen makeField(int flags, Type type, String name, Object value) {
        return new FieldGen(flags, type, name, value);
    }

    public static MethodGen makeMethod(int flags, String name, Type returnType, Type[] argTypes, String[] argNames, Type[] throwTypes) {
        return new MethodGen(flags, name, returnType, argTypes, argNames, throwTypes);
    }

}
