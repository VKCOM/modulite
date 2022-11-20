<?php

namespace Module\HideMethod;

class Some {
  public static function publicStatic() {}

  public function publicNotStatic() {}

  private static function privateStatic() {}

  private static function privateNotStatic() {}
}
