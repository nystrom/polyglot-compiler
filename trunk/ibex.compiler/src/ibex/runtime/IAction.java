/**
 * 
 */
package ibex.runtime;

public interface IAction<T> {
    T apply(IMatchContext context) throws MatchFailureException;
}