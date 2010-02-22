/**
 * 
 */
package polyglot.bytecode.rep.asm;

import org.objectweb.asm.tree.FieldNode;

import polyglot.bytecode.rep.IFieldGen;
import polyglot.bytecode.types.Type;

public class FieldGen implements IFieldGen {
    int flags;
    String name;
    Type type;
    Object value;
    
    FieldNode fn() {
        return new FieldNode(flags, name, type.desc(), null, value);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FieldGen ");
        sb.append(type);
        sb.append(" ");
        sb.append(name);
        return sb.toString();
    }

    public FieldGen(int flags, Type type, String name, Object value) {
        this.flags = flags;
        this.name = name;
        this.type = type;
        this.value = value;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IFieldGen#getFlags()
     */
    public int getFlags() {
        return flags;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IFieldGen#setFlags(int)
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IFieldGen#getName()
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IFieldGen#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IFieldGen#getType()
     */
    public Type getType() {
        return type;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IFieldGen#setType(polyglot.bytecode.types.Type)
     */
    public void setType(Type type) {
        this.type = type;
    }
}