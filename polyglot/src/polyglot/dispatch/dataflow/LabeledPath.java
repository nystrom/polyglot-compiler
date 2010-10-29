package polyglot.dispatch.dataflow;

import polyglot.types.Name;

public class LabeledPath<I extends FlowItem<I>> extends Path<I> {
    Name label;

    LabeledPath(polyglot.dispatch.dataflow.Path.Kind kind, Name label, I item) {
	super(kind, item);
	this.label = label;
    }

    public Name label() {
	// TODO Auto-generated method stub
	return null;
    }

    public boolean equals(Object o) {
	if (this == o)
	    return true;
	if (o instanceof LabeledPath<?>) {
	    LabeledPath<?> p = (LabeledPath<?>) o;
	    if (this.kind() != p.kind())
		return false;
	    if (!this.item.equals(p.item))
		return false;
	    if (this.label.equals(p.label))
		return false;
	    return true;
	}
	return false;
    }

    public int hashCode() {
	return kind.ordinal() + label.hashCode() + item.hashCode();
    }

    public String toString() {
	return kind.toString() + "(" + label + ", " + item + ")";
    }
}

