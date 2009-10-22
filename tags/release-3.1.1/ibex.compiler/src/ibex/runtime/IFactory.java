package ibex.runtime;

/** Interface for parsers that provide a factory for the output type. */
public interface IFactory<OutputType> extends IAuto<OutputType> {
    Object factory();
}
