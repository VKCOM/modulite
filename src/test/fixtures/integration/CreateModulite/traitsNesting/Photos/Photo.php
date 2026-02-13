<?php

namespace Photos;
use Engines\TestEngine;
class Photo {
  use \Engines\LikesEngineTrait;

  public static function printPhotoLikes() {
    self::print();
    TestEngine::tester();
  }
}
