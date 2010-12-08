public class BadFinalInit9 {
    
    void foo() {
        final int i = 3;
        i = 4;
    }
    
    public BadFinalInit9() { super(); }
}
