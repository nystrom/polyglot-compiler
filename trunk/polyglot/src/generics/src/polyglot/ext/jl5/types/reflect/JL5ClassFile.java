package polyglot.ext.jl5.types.reflect;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import polyglot.ext.jl5.types.AnnotationElemInstance;
import polyglot.ext.jl5.types.JL5Flags;
import polyglot.ext.jl5.types.JL5LazyClassInitializer;
import polyglot.ext.jl5.types.JL5ParsedClassType;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.frontend.ExtensionInfo;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.reflect.Attribute;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.InnerClasses;
import polyglot.types.reflect.Method;
import polyglot.types.reflect.JL5Field;

public class JL5ClassFile extends ClassFile implements JL5LazyClassInitializer {

    protected Signature signature;
    
    public JL5ClassFile(File classFileSource, byte[] code, ExtensionInfo ext){
        super(classFileSource, code, ext);
    }

    public void initEnumConstants(JL5ParsedClassType ct){
        JL5TypeSystem ts = (JL5TypeSystem)ct.typeSystem();
        
        for (int i = 0; i < fields.length; i++){
            if ((fields[i].modifiers() & JL5Flags.ENUM_MOD) != 0) {
                FieldInstance fi = fields[i].fieldInstance(ts, ct);
                ct.addEnumConstant(ts.enumInstance(ct.position(), ct, fi.flags(), fi.name(), ct));
            }
        }
    }

    public void initAnnotations(JL5ParsedClassType ct){
        JL5TypeSystem ts = (JL5TypeSystem)ct.typeSystem();

        for (int i = 0; i < methods.length; i++){
            MethodInstance mi = methods[i].methodInstance(ts, ct);
            AnnotationElemInstance ai = ts.annotationElemInstance(ct.position(), ct, mi.flags(), mi.returnType(), mi.name(), ((JL5Method)methods[i]).hasDefaultVal());
            ct.addAnnotationElem(ai);
        }
    }
    
      
    /* (non-Javadoc)
     * @see polyglot.types.LazyClassInitializer#initInterfaces(polyglot.types.ParsedClassType)
     */
    public void initInterfaces(ParsedClassType ct) {
        if ((ct instanceof JL5ParsedClassType) && (signature != null)) {
            //JL5ParsedClassType j5ct = (JL5ParsedClassType) ct;
            for (Iterator it = signature.classSignature.interfaces.iterator(); it.hasNext();) {
                Type iface = (Type) it.next();
                ct.addInterface(iface);
            }
        }
        else 
            super.initInterfaces(ct);
    }

    public ParsedClassType type(TypeSystem ts) throws SemanticException {
    
        JL5ParsedClassType t = (JL5ParsedClassType)super.type(ts);
        if (signature != null){
            try {
                signature.parseClassSignature(ts, t.position());
            }
            catch(IOException e){
            }

            t.typeVariables(signature.classSignature.typeVars());
            t.superType(signature.classSignature.superType());
        }
	t.superType(((JL5TypeSystem)ts).rawifyBareGenericType(t.superType()));
        return t;
    }
    
    
    
    public void readAttributes(DataInputStream in) throws IOException {
        int numAttributes = in.readUnsignedShort();
        attrs = new Attribute[numAttributes];
    
        for (int i = 0; i < numAttributes; i++){
            int nameIndex = in.readUnsignedShort();
            int length = in.readInt();
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

    @Override
    public Method createMethod(DataInputStream in) throws IOException {
        Method m = new JL5Method(in, this);
        m.initialize();
        return m;
    }

    public JL5Field createField(DataInputStream in) throws IOException {
	JL5Field f = new JL5Field(in, this);
	f.initialize();
	return f;
    }


}
