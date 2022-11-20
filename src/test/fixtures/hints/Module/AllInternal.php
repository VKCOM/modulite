<?php

namespace Module\AllInternal;

class Some {
}

interface Other {
  static function interfaceMethod();
}

define("ALL_INTERNAL_NAME", 0);

const SOME = 0, OTHER = 0;

$AllInternalGlobal = 10;

class TestClass extends Some implements Other {
  public const CONSTANT = 0;
  public $field = 0;
  public static $staticField = 0;

  public static function interfaceMethod() {
  }

  private function privateFunction() {}
}
