package polyglot.ext.jl5.types.reflect;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

import polyglot.types.reflect.Attribute;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.Constant;
import polyglot.types.reflect.Exceptions;
import polyglot.types.reflect.Method;
/**
 * Add support for annotations and signature when reading a method from a .class
 */
public class JL5Method extends Method{

	protected boolean defaultVal;
	protected Signature signature;

	public JL5Method(DataInputStream in, ClassFile clazz) {
		super(in, clazz);
	}

	/**
	 * Add support for annotations and signature
	 */
	public void initialize() throws IOException {
		modifiers = in.readUnsignedShort();

		// Bridge method 0x0040
		if ((modifiers & 64) != 0) {
            synthetic = true;
		}

        // Synthetic method 0x0020 (can also be specified as an attribute)
		if ((modifiers & 32) != 0) {
            synthetic = true;
		}

		name = in.readUnsignedShort();
		type = in.readUnsignedShort();

		int numAttributes = in.readUnsignedShort();

		attrs = new Attribute[numAttributes];
		for (int i = 0; i < numAttributes; i++) {
			int nameIndex = in.readUnsignedShort();
			int length = in.readInt();
			Constant name = clazz.getConstants()[nameIndex];

			if (name != null){
				if ("Exceptions".equals(name.value())) {
					exceptions = new Exceptions(clazz, in, nameIndex, length);
					attrs[i] = exceptions;
				}
				if ("Synthetic".equals(name.value())) {
					synthetic = true;
				}
				if ("AnnotationDefault".equals(name.value())){
					defaultVal = true;
				}
				if("Signature".equals(name.value())){
					signature = new Signature(in, nameIndex, length, clazz);
					attrs[i] = signature;
				}
			}

			if (attrs[i] == null){
				long n = in.skip(length);
				if (n != length){
					throw new EOFException();
				}
			}
		}
	}

    public Signature getSignature() {
    	return signature;
    }

	public boolean hasDefaultVal(){
		return defaultVal;
	}
}

