package com.siyeh.igtest.style.unnecessary_tostring;

public class UnnecessaryToString {

    String foo(Object o) {
        return "star" + o.<warning descr="Unnecessary 'toString()' call">toString</warning>();
    }

    String bar() {
        char[] cs = {'!'};
        return "wars" + cs.toString();
    }

    void fizzz(Object o) {
        boolean c = true;
        System.out.println(o.toString() + c);
        System.out.println((o.toString()) + c);
    }

    void polyadic(Object s) {
      s = "abc" + s.<warning descr="Unnecessary 'toString()' call">toString</warning>() + "efg";
    }

    void printStream(Object o) {
        System.out.print(o.<warning descr="Unnecessary 'toString()' call">toString</warning>());
    }

    void builder(StringBuilder builder, Object o) {
        builder.append(o.<warning descr="Unnecessary 'toString()' call">toString</warning>());
    }

  String self() {
    return toString();
  }

  public static void main22(String[] args) {
    foo(args[0].toString());
  }

  static void foo(String s) {
    System.out.println(s);
  }

  class A {
    public String toString() {
      return "A";
    }
  }
  class B extends A {
    public String toString() {
      return "B" + super.toString();
    }
  }

  void exception() {
    try {

    } catch (RuntimeException e) {
      org.slf4j.LoggerFactory.getLogger(UnnecessaryToString.class).info("this: {}", e.toString());
    } catch (Exception | Error e) {
      org.slf4j.LoggerFactory.getLogger(UnnecessaryToString.class).info("this: {}", e.toString());
    }
  }

  void format() {
    Integer number = 1;
    String example = String.format("prefix%s", number.<warning descr="Unnecessary 'toString()' call">toString</warning>());
    System.out.printf("Hello %s", number.<warning descr="Unnecessary 'toString()' call">toString</warning>());
    System.out.printf(number.toString(), "Hello %s");
    System.out.printf(java.util.Locale.getDefault(), number.toString(), "Hello %s");
    System.out.printf(java.util.Locale.getDefault(), "Hello %s", number.<warning descr="Unnecessary 'toString()' call">toString</warning>());
  }
}