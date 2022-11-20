<?php

namespace Module\AllExport;

class Some {
}

interface Other {
  static function interfaceMethod();
}

define("ALL_EXPORT_NAME", 0);

const SOME = 0, OTHER = 0;

$AllExportGlobal = 10;

class TestClass extends Some implements Other {
  public const CONSTANT = 0;
  public $field = 0;
  public static $staticField = 0;

  public static function interfaceMethod() {}

  private function privateFunction() {}
}
