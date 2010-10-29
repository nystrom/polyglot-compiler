package polyglot.dispatch.dataflow;


public class NormalPath<I extends FlowItem<I>> extends Path<I> {
    NormalPath(polyglot.dispatch.dataflow.Path.Kind kind, I item) {
	super(kind, item);
    }

    public boolean equals(Object o) {
	if (this == o)
	    return true;
	if (o instanceof NormalPath<?>) {
	    NormalPath<?> p = (NormalPath<?>) o;
	    if (this.kind() != p.kind())
		return false;
	    if (!this.item.equals(p.item))
		return false;
	    return true;
	}
	return false;
    }

    public int hashCode() {
	return kind.ordinal() + item.hashCode();
    }

    public String toString() {
	return kind.toString() + "(" + item + ")";
    }
}

