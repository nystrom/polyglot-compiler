package polyglot.dispatch.dataflow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import polyglot.dispatch.dataflow.Path.Kind;
import polyglot.types.ClassDef;
import polyglot.types.Name;

public class PathSet<I extends FlowItem<I>> extends HashSet<Path<I>> {
    public I normalItem() {
	I j = null;
	for (Path<I> p : this) {
	    if (p.kind() == Path.Kind.NORMAL)
		if (j == null)
		    j = p.item();
		else
		    j = j.mergeWith(p.item());
	}
	return j;
    }

    public I trueItem() {
	I j = null;
	for (Path<I> p : this) {
	    if (p.kind() == Path.Kind.TRUE)
		if (j == null)
		    j = p.item();
		else
		    j = j.mergeWith(p.item());
	}
	if (j == null)
	    return normalItem();
	return j;
    }

    public I falseItem() {
	I j = null;
	for (Path<I> p : this) {
	    if (p.kind() == Path.Kind.FALSE)
		if (j == null)
		    j = p.item();
		else
		    j = j.mergeWith(p.item());
	}
	if (j == null)
	    return normalItem();
	return j;
    }

    public I breakItem() {
	I j = null;
	for (Path<I> p : this) {
	    if (p.kind() == Path.Kind.BREAK && p instanceof NormalPath<?>)
		if (j == null)
		    j = p.item();
		else
		    j = j.mergeWith(p.item());
	}
	return j;
    }

    public I breakItem(Name label) {
	I j = null;
	for (Path<I> p : this) {
	    if (p.kind() == Path.Kind.BREAK && p instanceof LabeledPath<?> && ((LabeledPath<?>) p).label() == label)
		if (j == null)
		    j = p.item();
		else
		    j = j.mergeWith(p.item());
	}
	return j;
    }

    public I continueItem() {
	I j = null;
	for (Path<I> p : this) {
	    if (p.kind() == Path.Kind.CONTINUE && p instanceof NormalPath<?>)
		if (j == null)
		    j = p.item();
		else
		    j = j.mergeWith(p.item());
	}
	return j;
    }

    public I continueItem(Name label) {
	I j = null;
	for (Path<I> p : this) {
	    if (p.kind() == Path.Kind.CONTINUE && p instanceof LabeledPath<?> && ((LabeledPath<?>) p).label() == label)
		if (j == null)
		    j = p.item();
		else
		    j = j.mergeWith(p.item());
	}
	return j;
    }

    public I exceptionalItem(ClassDef type) {
	I j = null;
	for (Path<I> p : this) {
	    if (p.kind() == Path.Kind.EXCEPTIONAL && p instanceof ExceptionalPath<?> && ((ExceptionalPath<?>) p).type() == type)
		if (j == null)
		    j = p.item();
		else
		    j = j.mergeWith(p.item());
	}
	return j;
    }
    
    PathSet<I> xunion(Path<I>... ps) {
	PathSet<I> s = new PathSet<I>();
	for (Path<I> p : ps) {
	    s.add(p);
	}
	return s;
    }

    PathSet<I> xunion(Collection<Path<I>> qs, Path<I>... ps) {
	PathSet<I> s = new PathSet<I>();
	for (Path<I> p : qs) {
	    s.add(p);
	}
	for (Path<I> p : ps) {
	    s.add(p);
	}
	return s;
    }
    
    PathSet<I> unionAbrupt(Collection<Path<I>> ps) {
	PathSet<I> s = new PathSet<I>();
	for (Path<I> p : ps) {
	    switch (p.kind()) {
	    case BREAK:
	    case CONTINUE:
	    case EXCEPTIONAL:
		s.add(p);
		break;
	    default:
		break;
	    }
	}
	return s;
    }
    PathSet<I> unionThrows(Collection<Path<I>> ps) {
	PathSet<I> s = new PathSet<I>();
	for (Path<I> p : ps) {
	    switch (p.kind()) {
	    case EXCEPTIONAL:
		s.add(p);
		break;
	    default:
		break;
	    }
	}
	return s;
    }
   
    PathSet<I> mergeWith(Collection<Path<I>> ps) {
	PathSet<I> s = new PathSet<I>();
	for (Path<I> p : ps) {
	    s.add(p);
	}
	return s;
    }
    
    PathSet<I> mergeWith(Path<I>... ps) {
	PathSet<I> s = new PathSet<I>();
	for (Path<I> p : ps) {
	    s.add(p);
	}
	return s;
    }
    
    static PathSet<?> empty = new PathSet();
    
    static <I extends FlowItem<I>> PathSet<I> emptySet() {
	return (PathSet<I>) empty;
    }
    
    static <I extends FlowItem<I>> PathSet<I> union(Path<I>... ps) {
	PathSet<I> s = new PathSet<I>();
	return s.mergeWith(ps);
    }
    
    PathSet<I> normalItem(I item) {
	PathSet<I> ps = new PathSet<I>();
	for (Path<I> p : this) {
	    if (p.kind() != Path.Kind.NORMAL)
		ps.add(p);
	}
	ps.add(Path.normalPath(item));
	return ps;
    }
    PathSet<I> breakItem(I item) {
	PathSet<I> ps = new PathSet<I>();
	for (Path<I> p : this) {
	    if (p.kind() != Path.Kind.BREAK && p instanceof NormalPath<?>)
		ps.add(p);
	}
	ps.add(Path.breakPath(item));
	return ps;
    }
    PathSet<I> breakItem(Name label, I item) {
	PathSet<I> ps = new PathSet<I>();
	for (Path<I> p : this) {
	    if (p.kind() != Path.Kind.BREAK && p instanceof LabeledPath<?> && ((LabeledPath<?>) p).label() == label)
		ps.add(p);
	}
	ps.add(Path.breakPath(label, item));
	return ps;
    }
    PathSet<I> continueItem(I item) {
	PathSet<I> ps = new PathSet<I>();
	for (Path<I> p : this) {
	    if (p.kind() != Path.Kind.CONTINUE && p instanceof NormalPath<?>)
		ps.add(p);
	}
	ps.add(Path.continuePath(item));
	return ps;
    }
    PathSet<I> continueItem(Name label, I item) {
	PathSet<I> ps = new PathSet<I>();
	for (Path<I> p : this) {
	    if (p.kind() != Path.Kind.CONTINUE && p instanceof LabeledPath<?> && ((LabeledPath<?>) p).label() == label)
		ps.add(p);
	}
	ps.add(Path.continuePath(label, item));
	return ps;
    }
    PathSet<I> trueItem(I item) {
	PathSet<I> ps = new PathSet<I>();
	for (Path<I> p : this) {
	    if (p.kind() != Path.Kind.TRUE)
		ps.add(p);
	}
	ps.add(Path.truePath(item));
	return ps;
    }
    PathSet<I> falseItem(I item) {
	PathSet<I> ps = new PathSet<I>();
	for (Path<I> p : this) {
	    if (p.kind() != Path.Kind.FALSE)
		ps.add(p);
	}
	ps.add(Path.falsePath(item));
	return ps;
    }
}
