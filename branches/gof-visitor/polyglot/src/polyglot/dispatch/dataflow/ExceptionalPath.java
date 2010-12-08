package polyglot.dispatch.dataflow;

import polyglot.types.ClassDef;

public class ExceptionalPath<I extends FlowItem<I>> extends Path<I> {
    ClassDef type;

    ExceptionalPath(ClassDef type, I item) {
	super(Kind.EXCEPTIONAL, item);
	this.type = type;
    }

    ClassDef type() {
	return type;
    }

    public boolean equals(Object o) {
	if (this == o)
	    return true;
	if (o instanceof ExceptionalPath<?>) {
	    ExceptionalPath<?> p = (ExceptionalPath<?>) o;
	    if (this.kind() != p.kind())
		return false;
	    if (!this.item.equals(p.item))
		return false;
	    if (this.type != p.type)
		return false;
	    return true;
	}
	return false;
    }

    public int hashCode() {
	return kind.ordinal() + type.hashCode() + item.hashCode();
    }

    public String toString() {
	return kind.toString() + "(" + type + ", " + item + ")";
    }
}
