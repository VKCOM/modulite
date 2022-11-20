<?php

namespace Feed\Infra;

define('DEF_STRINGS_1', 1);
define('DEF_STRINGS_2', 2);

class Strings {
  const STR_NAME   = 'name';
  const STR_HIDDEN = 'hidden';

  static function demo() {
    echo self::STR_NAME, ' ', self::STR_HIDDEN, ' ', Hidden::HIDDEN_1, Hidden::HIDDEN_2;
    Hidden::demo();
  }
}
