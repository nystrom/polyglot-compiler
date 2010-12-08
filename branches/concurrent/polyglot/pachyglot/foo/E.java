package foo;

import java.lang.*;

public class E {
    
    public int st(int a) {
        a = 0;
        java.lang.System.out.println(a);
        return a;
    }
    
    public static void main(java.lang.String[] args) {
        E l = new E();
        l.st(0);
    }
    
    public E() { super(); }
}
