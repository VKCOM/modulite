<?php

namespace Feed\Infra;

class Hidden {
  const HIDDEN_1 = 1;
  const HIDDEN_2 = 2;

  static function demo() {
    (function() {
      echo self::HIDDEN_2, "\n";
      echo DEF_POST_1, "\n";
//         ^^^^^^^^^^
//         error: restricted to use DEF_POST_1, it's internal in @feed
    })();
  }
}
