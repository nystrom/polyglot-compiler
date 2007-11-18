/**
 * 
 */
package polyglot.types;

import polyglot.frontend.GoalSet;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class ErrorRef_c<T extends TypeObject>  extends TypeObject_c implements Ref<T> {
      public ErrorRef_c(TypeSystem ts, Position pos) {
          super(ts, pos);
      }
      
      public T get() {
          throw new InternalCompilerError("Bad", position());
      }
      
      public T get(GoalSet view) {
          throw new InternalCompilerError("Bad", position());
      }
      
      public boolean nonnull() {
          return false;
      }
  }