package ibex.runtime;

public class MatchFailureException extends Exception {
    public MatchFailureException() { }

    public MatchFailureException(String message) {
        super(message);
    }
}
