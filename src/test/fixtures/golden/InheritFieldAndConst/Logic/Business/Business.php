<?php

namespace Logic\Business;

use Logic\Common\Language;

class Business {
  function logic() {
    $is_rus = 1 == Language::RUS;
    $is_eng = 3 == Language::$ENG;
  }
}
