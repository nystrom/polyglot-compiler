package ibex.lr;

abstract class Action {
    abstract int encode();

    public static final int ERROR = 0;
    public static final int SHIFT = 1;
    public static final int REDUCE = 2;
    public static final int ACCEPT = 3;
    public static final int OVERFLOW = 4;
    public static final int POS_LOOKAHEAD = 5;
    public static final int NEG_LOOKAHEAD = 6;
    
    // the bottom 29 bits are of an action table entry e indicate the following:
    // if action(e) == SHIFT, the state to transition to after the shift
    // if action(e) == REDUCE, the rule to reduce
    // if action(e) == ACCEPT, the nonterminal to accept
    // if action(e) == OVERFLOW, the entry in the action_overflow_table
    //                            containing the list of actions
    // 0 otherwise
    
    // these methods are really macros; the run-time compiler should inline
    // them                     
    static int encode(int type, int data) {
        return type << 29 | data & ~(7 << 29);
    }   
}
