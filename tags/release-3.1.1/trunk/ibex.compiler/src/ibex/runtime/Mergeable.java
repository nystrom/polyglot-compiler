package ibex.runtime;

public interface Mergeable {
    Mergeable merge(Mergeable o);
}
