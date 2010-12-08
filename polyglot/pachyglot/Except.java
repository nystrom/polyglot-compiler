class Except {
    int x;
    
    java.lang.Object m(Except a) {
        try {
            a.x = 0;
        }
        catch (java.lang.NullPointerException e) { return e; }
        return null;
    }
    
    public Except() { super(); }
}
