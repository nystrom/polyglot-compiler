public class BadFinalInit7 {
    final int i;
    
    BadFinalInit7() {
        super();
        i = 3;
    }
    
    void foo() { i = 4; }
}
