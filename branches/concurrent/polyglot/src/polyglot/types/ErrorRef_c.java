/**
 * 
 */
package polyglot.types;

import polyglot.types.Ref.Callable;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class ErrorRef_c<T> extends Ref_c<T> {
    String errorMessage;
    Position pos;

    public ErrorRef_c(TypeSystem ts, Position pos, String errorMessage) {
	this.pos = pos;
	this.errorMessage = errorMessage;
    }

    @Override
    public T get() {
	throw new InternalCompilerError(errorMessage, pos);
    }

    @Override
    public T getCached() {
	throw new InternalCompilerError(errorMessage, pos);
    }
}
