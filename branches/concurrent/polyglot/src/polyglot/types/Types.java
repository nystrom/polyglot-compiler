package polyglot.types;

import java.util.ArrayList;
import java.util.List;
import polyglot.ast.Node;
import polyglot.frontend.Globals;
import polyglot.util.InternalCompilerError;

public class Types {
	
	public static enum Granularity{
		OTHER,
		SOURCE_FILE_LEVEL,
		CLASS_LEVEL,
		METHOD_LEVEL,
		LOWER_LEVEL,
		EXPR
		
	};
	
	public static <T> T get(Ref<T> ref) {
		return ref != null ? ref.get() : null;
	}

	/** Return a reference to v. */
	public static <T> Ref<T> ref(T v) {
		if (v == null)
			return null;
		Ref_c<T> r = new Ref_c<T>(v);
		return r;
	}

	public static <T> Ref<T> lazyRefFuture() {
		// future and clock not set
		Ref_c<T> r = new Ref_c<T>();
		return r;
	}

	public static <T> Ref<T> lazyRefFuture(T defaultValue) {
		// future and clock not set
		Ref_c<T> r = new Ref_c<T>();
		r.updateSoftly(defaultValue);
		return r;
	}
	
	public static <T> Ref<T> lazyRefLazy() {
		// future and clock not set
		LazyRef_c<T> r = new LazyRef_c<T>();
		return r;
	}
	
	public static <T> Ref<T> lazyRefLazy(T defaultValue) {
		// future and clock not set
		LazyRef_c r = new LazyRef_c<T>(defaultValue);
		r.updateSoftly(defaultValue);
		return r;
	}

	public static <T> Ref<T> lazyRef(Granularity grain) {
		if (Globals.Options().use_future_at_level_all) {
			return lazyRefFuture();
		}
		
		else if((Globals.Options().use_future_at_level_class && 
					(grain == Granularity.CLASS_LEVEL || grain == Granularity.SOURCE_FILE_LEVEL ))  
			){
			return lazyRefFuture();	//need to change this, it is only to satisfy the compiler for now.	
		} 
		else if((Globals.Options().use_future_at_level_method && 
				(grain == Granularity.METHOD_LEVEL || 
						grain == Granularity.CLASS_LEVEL ||
						grain == Granularity.SOURCE_FILE_LEVEL ))
				){
			return lazyRefFuture();		
			
		}
		
		else if((Globals.Options().use_future_at_level_file && grain == Granularity.SOURCE_FILE_LEVEL)){
			return lazyRefFuture();		
		}
		//everyone else gets a lazy ref
		return lazyRefLazy(); //have to change this to lazyRefLazy
	}
	
	public static <T> Ref<T> lazyRef(T defaultValue, Granularity grain) {
		// future and clock not set
		
		if (Globals.Options().use_future_at_level_all) {
			return lazyRefFuture(defaultValue);
		}
		
		else if((Globals.Options().use_future_at_level_class && 
					(grain == Granularity.CLASS_LEVEL || grain == Granularity.SOURCE_FILE_LEVEL ))  
			){
			return lazyRefFuture(defaultValue);	//need to change this, it is only to satisfy the compiler for now.	
		} 
		else if((Globals.Options().use_future_at_level_method && 
				(grain == Granularity.METHOD_LEVEL || 
						grain == Granularity.CLASS_LEVEL ||
						grain == Granularity.SOURCE_FILE_LEVEL ))
				){
			return lazyRefFuture(defaultValue);		
			
		}
		
		else if((Globals.Options().use_future_at_level_file && grain == Granularity.SOURCE_FILE_LEVEL)){
			return lazyRefFuture(defaultValue);		
		}
			
		//everyone else gets a lazy ref
		return lazyRefLazy(); //have to change this to lazyRefLazy
	}

	

	

	public static <T> Ref<T> lazyRef(T defaultValue,
			final Ref.Callable<T> resolver, funicular.Clock clock) {
		Ref_c<T> r = new Ref_c<T>(resolver, clock);
		r.updateSoftly(defaultValue);
		return r;
	}

	public static <T> Ref<T> lazyRef(T defaultValue, final Runnable resolver,
			funicular.Clock clock) {
		Ref_c<T> r = new Ref_c<T>();
		r.updateSoftly(defaultValue);
		r.setResolver(resolver, clock);
		return r;
	}
}
