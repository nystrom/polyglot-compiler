public class If
{
   Object o;
   boolean b;

   public static final void main(String args[])
   {
      int i = 1;
      Object x = new Object();
      Object n = null;
      final Object N = null;

      if (i == 0)
        System.out.println("bad " + i + "==0");
      else
        System.out.println("ok");

      if (i == 1)
        System.out.println("ok");
      else
        System.out.println("bad " + i + "==1");

      if (i != 1)
        System.out.println("bad " + i + "!=1");
      else
        System.out.println("ok");

      if (i > 1)
        System.out.println("bad " + i + ">1");
      else
        System.out.println("ok");

      if (i < 1)
        System.out.println("bad " + i + "<1");
      else
        System.out.println("ok");

      if (i >= 1)
        System.out.println("ok");
      else
        System.out.println("bad " + i + ">=1");

      if (i <= 1)
        System.out.println("ok");
      else
        System.out.println("bad " + i + "<=1");

      if (null == x)
        System.out.println("bad " + "null==x");
      else
        System.out.println("ok");

      if (null != x)
        System.out.println("ok");
      else
        System.out.println("bad " + "null!=x");

      if (x == null)
        System.out.println("bad " + "x==null");
      else
        System.out.println("ok");

      if (x != null)
        System.out.println("ok");
      else
        System.out.println("bad " + "x!=null");

      if (n == x)
        System.out.println("bad " + "n==x");
      else
        System.out.println("ok");

      if (n != x)
        System.out.println("ok");
      else
        System.out.println("bad " + "n!=x");

      if (x == n)
        System.out.println("bad " + "x==n");
      else
        System.out.println("ok");

      if (x != n)
        System.out.println("ok");
      else
        System.out.println("bad " + "x!=n");

      if (N == x)
        System.out.println("bad " + "N==x");
      else
        System.out.println("ok");

      if (N != x)
        System.out.println("ok");
      else
        System.out.println("bad " + "n!=x");

      if (x == N)
        System.out.println("bad " + "x==N");
      else
        System.out.println("ok");

      if (x != N)
        System.out.println("ok");
      else
        System.out.println("bad " + "x!=N");
   }
}
