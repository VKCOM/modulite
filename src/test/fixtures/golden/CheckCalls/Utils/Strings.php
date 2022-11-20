<?php

namespace Utils;

use GlobalCl;

class Strings {
  static function normal() {
    Hidden::demo();
    $_ = new GlobalCl;
    GlobalCl::staticFn();
//            ^^^^^^^^
//            error: restricted to call GlobalCl::staticFn(), it's not required by @utils
  }

  static function hidden1() {
  }

  static function hidden2() {
  }
}
