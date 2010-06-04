public class Shortcut
{
   static public boolean bad() {
    System.out.println("bad");
    return false;
   }
   static public boolean t() {
    System.out.println("ok");
    return true;
   }
   static public boolean f() {
    System.out.println("ok");
    return false;
   }
        
   public static final void main(String args[])
   {
      boolean a = t() && f();
      System.out.println("false=" + a);
      boolean b = f() && bad();
      System.out.println("false=" + b);
      boolean c = t() && t();
      System.out.println("true=" + c);

      boolean d = t() || bad();
      System.out.println("true=" + d);
      boolean e = f() || t();
      System.out.println("true=" + e);
      boolean f = f() || f();
      System.out.println("false=" + f);
   }
}
