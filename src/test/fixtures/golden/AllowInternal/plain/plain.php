<?php

use Utils003\Hidden003;

define('PLAIN_CONST_PUB', 1);
define('PLAIN_CONST_HID', 2);

function plainPublic1() {
}

function plainPublic2() {
}

function plainHidden1() {
  plainHidden2();
}

function plainHidden2() {
  echo (function() {
    /** @var $i ?Hidden003 */
    $i = null;
  })();
}

