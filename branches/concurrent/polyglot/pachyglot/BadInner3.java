class BadInner3 {
    
    public BadInner3() { super(); }
}

class E extends BadInner3 {
    class D {
        
        BadInner3 m() { return BadInner3.this; }
        
        public D() { super(); }
    }
    
    
    public E() { super(); }
}
