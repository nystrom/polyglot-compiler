package polyglot.types;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.locks.ReentrantLock;

import polyglot.dispatch.PassthruError;
import polyglot.util.TypeInputStream;

/** Reference to a object. */
public class LazyRef_c<T> extends Object implements Ref<T>, Serializable {
	private boolean started;
	private boolean forced;
	private T v;
//	private polyglot.types.Futures.SettableFuture<T> future;
	private Ref.Callable<T> future;
	private Handler<T> handler;

	public LazyRef_c(T v) {
		this(v, false); // a lazy ref is not forced. It starts only when it is forced to start through force()
	}

	public LazyRef_c(T v, boolean forced) {
		this.v = v;
		this.forced = forced;
		this.started = forced;
		this.future = null;
		
		
	}

	public LazyRef_c() {
		this((Ref.Callable<T>) null, null);
	}

	public LazyRef_c(final Ref.Callable<T> callable, funicular.Clock clock) {
		
		this.v = null;
		this.forced = false;
		this.started = false;
		if (callable == null) {
			this.future = null;
		} else {
			if (clock == null) {
				this.future = callable;//Futures$.MODULE$.make(callable);
			} else {
				this.future = callable; //Futures$.MODULE$.makeClocked(clock, callable);
			}
		}
	}

	public boolean hasResolver() {
		try {
			lock.lock();
			return future != null;
		} finally {
			lock.unlock();
		}
	}

	public void start() {
		// lock.lock();
		// if (! started) {
		// started = true;
		// lock.unlock();
		// Types.pool.execute(future);
		// }
		// else {
		// lock.unlock();
		// }
	}

	/** Return true if the reference is not null. */
	public boolean forced() {
		return forced;
	}

	public T force() {
		return get();
	}

	boolean updating;

	ReentrantLock lock = new ReentrantLock();

	public T get() {
		try {
			lock.lock();
			if (!forced) {
				try {
					if (updating) {
						update(v);
					} else {
						updating = true;
						try {
							try {
								lock.unlock();
								T v = compute();
								update(v);
							} finally {
								lock.lock();
							}
						} catch (PassthruError e) {
							fail = (Exception) e.getCause();
							throw e;
						} catch (Exception e) {
							fail = e;
							throw new PassthruError(e);
						}
					}
				} finally {
					updating = false;
				}
			}
			if (fail != null)
				throw new PassthruError(fail);
			return v;
		} finally {
			lock.unlock();
		}
	}

	Exception fail;

	Throwable debugException = new Exception("Ref created");

	protected T compute() throws Exception {
		if (future == null) {
			throw new RuntimeException("No resolver for " + this,
					debugException);
		}
		try {
			return future.call();//.force();
		} finally {
			future = null;
		}
	}

	public T getCached() {
		return v;
	}

	/*
	 * Update the ref. If already forced, the new value must be >= the old value
	 * (for some ordering on T).
	 */
	public void update(T v) {
		try {
			lock.lock();
			updateSoftly(v);
			this.forced = true;
			if (this.handler != null) {
				try {
					lock.unlock();
					handler.handle(v);
				} catch (InvocationTargetException e) {
					throw new PassthruError(e.getCause());
				} catch (Exception e) {
					throw new PassthruError(e);
				} finally {
					lock.lock();
					handler = null;
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public synchronized void updateSoftly(T v) {
		try {
			lock.lock();
			this.v = v;
		} finally {
			lock.unlock();
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		assert forced() : "resolver for " + this + " not reached";
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();

		if (in instanceof TypeInputStream) {
			v = null;
		}
	}

	public void reset() {
		this.forced = false;
	}

	public void setResolver(Ref.Callable<T> callable, funicular.Clock clock) {
		try {
			lock.lock();
			this.future = callable;
//			if (this.future != null) {
//				this.future.setCallable(callable);
//			} else {
//				if (callable == null) {
//					this.future = null;
//				} else {
//					if (clock == null){
//						this.future = Futures$.MODULE$.make(callable);
//					}
//					else {
//						this.future = Futures$.MODULE$.makeClocked(clock,
//								callable);
//					}
//				}
//			}
		} finally {
			lock.unlock();
		}
	}

	public void setResolver(final Runnable runnable, funicular.Clock clock) {
		setResolver(new Ref.Callable<T>() {
			public T call() {
				runnable.run();
				assert forced : "not forced " + this + " after " + runnable;
				return getCached();
			}
		}, clock);
	}

	public void addHandler(final Handler<T> h) {
		if (this.handler == null) {
			this.handler = h;
		} else {
			final Handler<T> old = this.handler;
			this.handler = new Handler<T>() {
				public void handle(T t) throws Exception {
					old.handle(t);
					h.handle(t);
				}
			};
		}
	}

	public String toString() {
		if (v != null) {
			T o = getCached();
			if (o == null)
				return "null";
			return o.toString();
		}
		if(forced){
			return "<forced ref " + super.toString() + ">";
		}
		return "<unforced ref " + super.toString() + ">";
	}
}
