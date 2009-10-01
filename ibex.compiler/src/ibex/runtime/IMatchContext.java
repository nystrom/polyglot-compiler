package ibex.runtime;

public interface IMatchContext {
    <T> T applyRule(String key, IAction<T> rule) throws MatchFailureException;

    /** Get the failure object. Called from the generated parser. */
    IMatchResult fail();

    /** Save the current position on the stack. Must be matched with a pop (or restore). */
    void save();

    /** Save the current position on the stack, recording extra info to restore after a lookahead.  Must be matched with a pop (or restore).  Calls save(). */
    void saveForLookahead();

    /** Pop the position stack, returning the argument (the semantic action result) */
    <T> T accept(T o);
    boolean accept(boolean o);
    char accept(char o);
    byte accept(byte o);
    short accept(short o);
    int accept(int o);
    long accept(long o);
    float accept(float o);
    double accept(double o);

    /** Restore the previous position from the stack.  Calls pop(). */
    void restore();

    /** Pop the position stack. Do not update the current position. */
    void pop();
    
    char nextMatches(char ch)throws MatchFailureException;
    String nextMatches(CharSequence s) throws MatchFailureException;
    char nextMatchesRange(char lo, char hi)throws MatchFailureException;
    char nextMatchesAny()throws MatchFailureException;
    
}
