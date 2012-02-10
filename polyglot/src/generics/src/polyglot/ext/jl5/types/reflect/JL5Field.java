package polyglot.ext.jl5.types.reflect;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import polyglot.types.reflect.Attribute;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.Constant;
import polyglot.types.reflect.ConstantValue;
import polyglot.types.reflect.Field;

// this class is put here rather than in ext.jl5 because its base class types.reflect.Field has a package-protected
// constructor!
public class JL5Field extends Field {
    protected Signature signature;

    public JL5Field(DataInputStream in, ClassFile clazz) throws IOException {
        super(in, clazz);
    }
    
    /**
     * Overriden to handle Signature attributes
     */
    public void initialize() throws IOException {
        modifiers = in.readUnsignedShort();

        name = in.readUnsignedShort();
        type = in.readUnsignedShort();
        
        int numAttributes = in.readUnsignedShort();
        
        attrs = new Attribute[numAttributes];
        
        for (int i = 0; i < numAttributes; i++) {
            int nameIndex = in.readUnsignedShort();
            int length = in.readInt();
            
            Constant name = clazz.getConstants()[nameIndex];
            
            if (name != null) {
                if ("ConstantValue".equals(name.value())) {
                    constantValue = new ConstantValue(in, nameIndex, length);
                    attrs[i] = constantValue;
                }
                if ("Synthetic".equals(name.value())) {
                    synthetic = true;
                }
                if("Signature".equals(name.value())){
                    signature = new Signature(in, nameIndex, length, clazz);
                    attrs[i] = signature;
                }
            }
            
            if (attrs[i] == null) {
                long n = in.skip(length);
                if (n != length) {
                    throw new EOFException();
                }
            }
        }
    }

    public Signature getSignature() {
        return signature;
    }
}
