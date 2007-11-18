/**
 * 
 */
package polyglot.types;

import polyglot.util.Transformation;

public class MethodAsTypeTransform implements
        Transformation<MethodDef, MethodType> {
    public MethodType transform(MethodDef def) {
        return def.asType();
    }
}