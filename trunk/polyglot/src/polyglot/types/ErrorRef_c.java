/**
 * 
 */
package polyglot.types;

import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class ErrorRef_c<T> extends TypeObject_c implements Ref<T> {
      public ErrorRef_c(TypeSystem ts, Position pos) {
          super(ts, pos);
      }
      
      public T get() {
          throw new InternalCompilerError("Bad", position());
      }
      
      public T getCached() {
          throw new InternalCompilerError("Bad", position());
      }
      
      public boolean known() {
          return false;
      }

      public void update(T v) {
    	  throw new InternalCompilerError("Bad", position());
      }
      
      public void when(Handler<T> h) {
    	  throw new InternalCompilerError("Bad", position());
      }
  }
