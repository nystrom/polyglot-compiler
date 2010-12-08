package polyglot.dispatch.dataflow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import polyglot.types.ClassDef;
import polyglot.types.Name;

public abstract class Path<I extends FlowItem<I>> {
    public static enum Kind {
	NORMAL, EXCEPTIONAL, BREAK, CONTINUE, TRUE, FALSE
    }

    static <I extends FlowItem<I>> NormalPath<I> normalPath(I i) {
	return new NormalPath<I>(Path.Kind.NORMAL, i);
    }

    static <I extends FlowItem<I>> NormalPath<I> truePath(I i) {
	return new NormalPath<I>(Path.Kind.TRUE, i);
    }

    static <I extends FlowItem<I>> NormalPath<I> falsePath(I i) {
	return new NormalPath<I>(Path.Kind.FALSE, i);
    }

    static <I extends FlowItem<I>> ExceptionalPath<I> exceptionalPath(ClassDef t, I i) {
	return new ExceptionalPath<I>(t, i);
    }

    static <I extends FlowItem<I>> LabeledPath<I> breakPath(I i) {
	return new LabeledPath<I>(Path.Kind.BREAK, null, i);
    }

    static <I extends FlowItem<I>> LabeledPath<I> continuePath(I i) {
	return new LabeledPath<I>(Path.Kind.CONTINUE, null, i);
    }

    static <I extends FlowItem<I>> LabeledPath<I> breakPath(Name label, I i) {
	return new LabeledPath<I>(Path.Kind.BREAK, label, i);
    }

    static <I extends FlowItem<I>> LabeledPath<I> continuePath(Name label, I i) {
	return new LabeledPath<I>(Path.Kind.CONTINUE, label, i);
    }

    I item;
    Kind kind;

    Path(Kind kind, I item) {
	this.kind = kind;
	this.item = item;
    }

    Kind kind() {
	return kind;
    }

    I item() {
	return item;
    }

    public abstract boolean equals(Object o);

    public abstract int hashCode();

    public abstract String toString();
}

