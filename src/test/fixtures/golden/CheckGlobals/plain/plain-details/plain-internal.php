<?php

function internal() {
  static $ss;
  $cb = function() {
    global $glob;
//         ^^^^^
//         error: restricted to use global $glob, it's not required by @plain
    $glob += 2;
  };
  $cb();
}
