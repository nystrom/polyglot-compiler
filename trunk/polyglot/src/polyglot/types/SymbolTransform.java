/**
 * 
 */
package polyglot.types;

import polyglot.util.Transformation;

public class SymbolTransform<T extends Def> implements Transformation<T,Symbol<T>> {
    public Symbol<T> transform(T o) {
        return o.symbol();
    }
}