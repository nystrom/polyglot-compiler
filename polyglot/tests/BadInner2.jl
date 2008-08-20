interface BadInner2 {
  	static class Foo { }
  	class Bar { }
}

class C implements BadInner2 {
  Object o = this.new Foo(); // error: Foo is static
  Object p = this.new Bar(); // error: Bar is implicitly static
}
