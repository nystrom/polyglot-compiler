class BadForwardRef {
    final static int x = y;
    final static int y = 3;
    
    public BadForwardRef() { super(); }
}
