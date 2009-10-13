/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */

class TA7 {
  private static int int_3 = -3;
  private static int int1 = 1;
  private static int int3 = 3;
  private static int int5 = 5;
  private static int int33 = 33;
  private static int int65 = 65;
  private static long long_3 = -3;
  private static long long_1 = -1;
  private static long long0 = 0;
  private static long long2 = 2;
  private static long long10000000000 = 10000000000L;
  private static long long0x0110000000000011 = 0x0110000000000011L;
  private static long long0x1010000000000101 = 0x1010000000000101L;
  private static long long0xBEEFBEEFCAFEBABE = 0xBEEFBEEFCAFEBABEL;

  public static void main(String[] args) {
    System.out.println();
    System.out.println("-- remTest --");
    remTest();
  }

  private static float float0 = 0.0f;
  private static float float0_9 = 0.9f;
  private static float float1 = 1.0f;
  private static float float1_5 = 1.5f;
  private static float float2 = 2.0f;
  private static float float_maxint = (float)Integer.MAX_VALUE;
  private static float float_minint = (float)Integer.MIN_VALUE;
  private static double double0 = 0.0f;
  private static double double1 = 1.0f;
  private static double double2 = 2.0f;
  private static double double_maxint = (double)Integer.MAX_VALUE;
  private static double double_minint = (double)Integer.MIN_VALUE;
  private static float float_maxlong = (float)Long.MAX_VALUE;
  private static float float_minlong = (float)Long.MIN_VALUE;
  private static double double_maxlong = (double)Long.MAX_VALUE;
  private static double double_minlong = (double)Long.MIN_VALUE;

  private static void remTest() {
    rem(+2, +3);
    rem(+2, -3);
    rem(-2, +3);
    rem(-2, -3);
  }

  private static void rem(final double a, final double b) {
    System.out.println(a + "  /  " + b + "=" + Long.toHexString(Double.doubleToLongBits(a / b)));
    System.out.println(a + "  %  " + b + "=" + Long.toHexString(Double.doubleToLongBits(a % b)));
    System.out.println(a + " rem " + b + "=" + Long.toHexString(Double.doubleToLongBits(Math.IEEEremainder(a, b))));
    System.out.println();
  }
}
