package polyglot.ext.carray.types;

import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.types.*;
import java.util.*;

/**
 * A <code>ConstArrayType</code> represents an array of base java types,
 * whose elements cannot change after initialization.
 */
public class ConstArrayType_c extends ArrayType_c implements ConstArrayType
{
    protected boolean isConst;

    /** Used for deserializing types. */
    protected ConstArrayType_c() { }

    public ConstArrayType_c(TypeSystem ts, Position pos, Type base, boolean isConst) {
        super(ts, pos, base);
        this.isConst = isConst;
    }

    public String toString() {
        return base.toString() + (isConst ? " const" : "") + "[]";
    }

    public boolean equals(Object o) {
        if (o instanceof ConstArrayType) {
            ConstArrayType t = (ConstArrayType) o;
            return t.isConst() == isConst && ts.isSame(base, t.base());
        }

        if (o instanceof ArrayType) {
            ArrayType t = (ArrayType) o;
            return ! isConst && ts.isSame(base, t.base());
        }

        return false;
    }

    public boolean isConst() {
        return isConst;
    }

    public boolean isImplicitCastValidImpl(Type toType) {
        if (toType instanceof ConstArrayType &&
            ((ConstArrayType) toType).isConst()) {
            // int const[] = int[] 
            return super.isImplicitCastValidImpl(toType);
        }

        // From this point, toType is not a const array

        if (! isConst) {
            if (toType.isArray()) {
                // non-const arrays are invariant.
                return ts.isSame(this, toType);
            }
            else {
                // Object = int[] 
                return super.isImplicitCastValidImpl(toType);
            }
        }

        return false;
    }
}
