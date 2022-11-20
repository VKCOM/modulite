<?php

namespace Feed\Infra;

class Strings {
  static function demo() {
    global $glob,
//         ^^^^^
//         error: restricted to use global $glob, it's not required by @feed/infra
           $somehow;
//         ^^^^^^^^
//         error: restricted to use global $somehow, it's not required by @feed/infra
  }
}
