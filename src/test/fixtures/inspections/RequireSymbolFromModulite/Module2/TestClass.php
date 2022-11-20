<?php

namespace Module2;

class TestClass {
  public function __construct() {
    $_ = new Module2Class();
    $_ = new \Module1\TestClass();
//           ^^^^^^^^^^^^^^^^^^
//           error: restricted to use Module1\TestClass, @require-symbol-from-modulite is not required by @require-symbol-from-modulite2
  }
}
