package polyglot.types;


/**
 * A reference to a value of type T. The value may be computed lazily. Optional
 * handlers can be installed to be invoked when the value is updated.
 * 
 * The ref can be updated multiple times. Values should be monotonic; that is,
 * the new value should be greater than the old value according to some
 * (partial) ordering. This is not enforced, however.
 */
public interface Ref<T> {
    public static interface Callable<T> extends java.util.concurrent.Callable<T> {
	T call();
    }

    public static interface Handler<T> {
	void handle(T t) throws Exception;
    }
    
    void start();

    /** Compute the value using the installed resolver, if not already computed. */
    public T get();

    /** Get the current value.  Will not invoke a resolver.  May return null if the ref has never been updated. */
    public T getCached();

    /** Set the value, invoking any installed handlers. */
    public void update(T v);
    
    /** Set the value, but don't mark the ref forced. */
    public void updateSoftly(T v);

    /** Returns true if the value has been forced (i.e., computed and not reset). */
    public boolean forced();

    /** Install a resolver to compute the value.  The compute method of the Callable must return the new value.  Implies reset. */
    public void setResolver(Ref.Callable<T> callable, funicular.Clock clock);

    /** Install a resolver to compute the value.  The Runnable must update the value.  Implies reset. */
    public void setResolver(Runnable runnable, funicular.Clock clock);

    /** Install a new handler.  Will be invoked after previously installed handlers. */
    public void addHandler(Handler<T> handler);

    /**
     * Cause the value to be recomputed on the next call to get.
     */
    public void reset();
    
    public boolean hasResolver();

}
