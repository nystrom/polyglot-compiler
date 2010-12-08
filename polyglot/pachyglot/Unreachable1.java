public class Unreachable1 {
    
    void m1() {
        return;
        java.lang.System.out.println("Never happens.");
    }
    
    public Unreachable1() { super(); }
}
