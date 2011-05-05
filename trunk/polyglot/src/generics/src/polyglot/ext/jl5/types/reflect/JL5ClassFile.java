package polyglot.ext.jl5.types.reflect;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import polyglot.frontend.ExtensionInfo;
import polyglot.types.reflect.Attribute;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.InnerClasses;
import polyglot.types.reflect.Method;

public class JL5ClassFile extends ClassFile {

    protected Signature signature;
    
    public JL5ClassFile(File classFileSource, byte[] code, ExtensionInfo ext){
        super(classFileSource, code, ext);
    }

    /**
     * We skip all attributes except:
     * InnerClasses: why ? 
     * Signature: necessary to build generics info
     */
    @Override
    public void readAttributes(DataInputStream in) throws IOException {
        int numAttributes = in.readUnsignedShort();
        attrs = new Attribute[numAttributes];
    
        for (int i = 0; i < numAttributes; i++){
            int nameIndex = in.readUnsignedShort();
            int length = in.readInt();
            //CHECK why do we need InnerClasses here ?
            if ("InnerClasses".equals(constants[nameIndex].value())) {
                innerClasses = new InnerClasses(in, nameIndex, length);
                attrs[i] = innerClasses;
            }
            else if ("Signature".equals(constants[nameIndex].value())){
                signature = new Signature(in, nameIndex, length, this);
                attrs[i] = signature;
            }
            else {
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
    
    @Override
    public Method createMethod(DataInputStream in) throws IOException {
        Method m = new JL5Method(in, this);
        m.initialize();
        return m;
    }
}
