/**
 * 
 */
package polyglot.bytecode.rep.asm;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import polyglot.bytecode.rep.IClassGen;
import polyglot.bytecode.rep.IFieldGen;
import polyglot.bytecode.rep.IMethodGen;
import polyglot.bytecode.types.Type;
import polyglot.types.Name;
import polyglot.types.QName;

public class ClassGen implements IClassGen {
    public ClassGen(String name, Type superClass, String fileName, int flags, Type[] interfaces) {
        this.name = name;
        this.superClass = superClass;
        this.sourceFile = fileName;
        this.flags = flags;
        this.interfaces = Arrays.asList(interfaces);
        this.methods = new ArrayList<IMethodGen>();
        this.fields = new ArrayList<IFieldGen>();
        this.inner = new ArrayList<IClassGen>();
    }

    public String toString() {
        return "ClassGen " + name;
    }
    
    public byte[] bytes() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String[] interfaceNames = new String[interfaces.size()];
        for (int i = 0; i < interfaces.size(); i++) {
            interfaceNames[i] = interfaces.get(i).className();
        }
        cw.visit(Opcodes.V1_5, flags, name, null, superClass.className(), interfaceNames);
        cw.visitSource(sourceFile, null);
//        cn.version = Opcodes.V1_4;
//        cn.access = flags;
//        cn.name = name;
//        cn.superName = superClass.className();
//        cn.interfaces = new ArrayList<String>();
//        for (Type i : interfaces) {
//            cn.interfaces.add(i.className());
//        }
//        cn.fields = new ArrayList<FieldNode>(this.fields.size());
//        for (IFieldGen fg : fields) {
//            FieldGen f = (FieldGen) fg;
//            cn.fields.add(f.fn());
//        }
//        cn.methods = new ArrayList<MethodNode>(this.methods.size());
//        for (IMethodGen mg : methods) {
//            MethodGen m = (MethodGen) mg;
//            cn.methods.add(m.mn());
//        }
//        cn.sourceFile = sourceFile;
//        ClassWriter cw = new ClassWriter(0);
//        cn.accept(cw);

        for (IFieldGen fg : fields) {
            FieldGen f = (FieldGen) fg;
            FieldVisitor w = cw.visitField(f.getFlags(), f.getName(), f.getType().desc(), null, f.value);
            w.visitEnd();
        }
        for (IMethodGen mg : methods) {
            MethodGen m = (MethodGen) mg;
            MethodNode mn = m.mn();
            String s = Bytecodes.insnListToString(mn.instructions);
            mn.accept(cw);
        }
        for (IClassGen cg : inner) {
            cw.visitInnerClass(cg.getName(), name, cg.name().toString(), cg.getFlags() & ~Modifier.SYNCHRONIZED);
        }
        cw.visitEnd();
        return cw.toByteArray();
    }
    
    public String getName() {
        return name;
    }
    
    public int getFlags() {
        return flags;
    }
    
    public QName fullName() {
        return QName.make(name.replace('/', '.'));
    }
    public Name name() {
        return fullName().name();
    }
    
    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IClassGen#type()
     */
    public Type type() {
        return Type.typeFromClassName(name);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IClassGen#methods()
     */
    public List<IMethodGen> methods() {
        return methods;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IClassGen#fields()
     */
    public List<IFieldGen> fields() {
        return fields;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IClassGen#innerClasses()
     */
    public List<IClassGen> innerClasses() {
        return inner;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IClassGen#addMethod(polyglot.bytecode.rep.MethodGen)
     */
    public void addMethod(IMethodGen m) {
        methods.add(m);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IClassGen#addField(polyglot.bytecode.rep.FieldGen)
     */
    public void addField(IFieldGen m) {
        fields.add(m);
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IClassGen#addInnerClass(polyglot.bytecode.rep.ClassGen)
     */
    public void addInnerClass(IClassGen m) {
        inner.add(m);
    }

    int flags;
    String name;
    Type superClass;
    List<Type> interfaces;
    List<IMethodGen> methods;
    List<IFieldGen> fields;
    List<IClassGen> inner;
    String sourceFile;
}