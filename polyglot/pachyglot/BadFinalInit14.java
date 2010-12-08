class BadFinalInit14 {
    final int N;
    
    BadFinalInit14() {
        super();
        N = 0;
    }
    
    class Inner {
        
        Inner() {
            super();
            N = 0;
        }
    }
    
}
