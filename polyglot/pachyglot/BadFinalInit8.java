public class BadFinalInit8 {
    final int i = 3;
    
    BadFinalInit8() {
        super();
        i = 4;
    }
}
