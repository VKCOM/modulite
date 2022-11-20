<?php

require_once __DIR__ . '/plain/plain.php';

function globalDemo() {
  Utils\Strings::hidden2();
//               ^^^^^^^
//               error: restricted to call Utils\Strings::hidden2(), it's internal in @utils
  Utils\Strings::normal();
  plainHidden2();
//^^^^^^^^^^^^
//error: restricted to call plainHidden2(), it's internal in @plain
  $_ = new Utils\Hidden;
//         ^^^^^^^^^^^^
//         error: restricted to use Utils\Hidden, it's internal in @utils

  $_ = new Utils\HiddenWithConstructor();
//         ^^^^^^^^^^^^^^^^^^^^^^^^^^^
//         error: restricted to use Utils\HiddenWithConstructor, it's internal in @utils

}

globalDemo();
Feed111\Post111::demo();
Messages111\User111::demo();
Feed111\Post111::demoRequireUnexported();
