<?php

namespace Module;

class Foo {
  public static function publicStatic() {}

  private static function privateStatic() {}

  public function publicNotStatic() {}

  private static function privateNotStatic() {}
}
