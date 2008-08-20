class BadStaticContext {
  class Z {
    Z(Object x) { }
  }
  class Y extends Z {
    Y() {
	super(x);
    }

    Object x;
  }
}
