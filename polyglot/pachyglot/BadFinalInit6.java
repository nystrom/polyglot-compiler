public class BadFinalInit6 {
    final int i;
    
    BadFinalInit6() {
        super();
        i = 3;
        i = 4;
    }
}
