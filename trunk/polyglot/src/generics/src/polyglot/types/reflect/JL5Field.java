package polyglot.types.reflect;

import polyglot.types.*;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.JL5Flags;
import java.util.*;
import java.io.*;

// this class is put here rather than in ext.jl5 because its base class types.reflect.Field has a package-protected
// constructor!
public class JL5Field extends Field {
    
    public FieldInstance fieldInstance(TypeSystem ts, ClassType ct) {
	FieldInstance fi = super.fieldInstance(ts, ct);
	fi = fi.type(((JL5TypeSystem)ts).rawifyBareGenericType(fi.type()));
	if (JL5Flags.isEnumModifier(fi.flags())) {
	    fi = ((JL5TypeSystem)ts).enumInstance(ct.position(), ct, fi.flags(), fi.name(), (ParsedClassType) ct);
	}
	return fi;
    }

    public JL5Field(DataInputStream in, ClassFile clazz)
        throws IOException
    {
        super(in, clazz);
    }

}
