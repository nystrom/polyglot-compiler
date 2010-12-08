public class BadFinalInit4 {
    
    void foo(final int i) { if (false) { i = 1; } }
    
    public BadFinalInit4() { super(); }
}
