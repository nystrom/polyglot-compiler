class C {
    
    B() { super(); }
    
    C m() {
        return new C() {
            
            C() { super(); }
        };
    }
}
