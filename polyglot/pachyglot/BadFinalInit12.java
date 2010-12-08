class BadFinalInit12 {
    final int x;
    
    BadFinalInit12() {
        super();
        BadFinalInit12 a = this;
        a.x = 0;
    }
}
