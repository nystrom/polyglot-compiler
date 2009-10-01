/**
 * 
 */
package ibex.runtime;

public final class Failure implements IMatchResult {
    private MatchFailureException e;

    Failure(MatchFailureException e) {
        this.e = e;
    }

    public MatchFailureException exception() {
        return e;
    }

    public String toString() {
        return "FAIL";
    }
}