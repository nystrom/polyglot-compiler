public class BadFinalInit10 {
    
    BadFinalInit10() {
        super();
        i = 2;
    }
    
    final int i = 0;
}
