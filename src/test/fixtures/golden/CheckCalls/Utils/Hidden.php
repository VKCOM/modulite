<?php

namespace Utils;

class Hidden {
  static function demo() {
    self::demo2();
  }

  static function demo2() {
    if (0) {
      self::demo();
    }
    if (0) {
      Strings::normal();
    }
  }
}
