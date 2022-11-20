<?php

global $in_plain;
//     ^^^^^^^^^
//     error: restricted to use global $in_plain, it's not required by @plain
if (!isset($in_plain)) {
  $in_plain = 0;
}

function plain() {
  require_once __DIR__ . '/plain-details/plain-internal.php';
  internal();
}

