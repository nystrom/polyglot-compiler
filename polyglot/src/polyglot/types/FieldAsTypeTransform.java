/**
 * 
 */
package polyglot.types;

import polyglot.util.Transformation;

public class FieldAsTypeTransform implements
        Transformation<FieldDef, FieldType> {
    public FieldType transform(FieldDef def) {
        return def.asType();
    }
}