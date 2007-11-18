/**
 * 
 */
package polyglot.types;

import polyglot.util.Transformation;

public class ConstructorAsTypeTransform implements
        Transformation<ConstructorDef, ConstructorType> {
    public ConstructorType transform(ConstructorDef def) {
        return def.asType();
    }
}