package polyglot.ext.jl5.types.reflect;

import polyglot.types.TypeSystem;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.ClassFileLazyClassInitializer;

public class JL5ClassFileLazyClassInitializer extends
		ClassFileLazyClassInitializer {

    protected boolean enumConstantInitialized;
    protected boolean annotationsInitialized;

	public JL5ClassFileLazyClassInitializer(ClassFile file, TypeSystem ts) {
		//CHECK need to implement JL5ClassFileLazyClassInitializer for enum and annotation
		super(file, ts);
	}
	
//	@Override
//	protected boolean initialized() {
//		return super.initialized() && enumConstantInitialized && annotationsInitialized;
//	}

}
