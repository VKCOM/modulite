<?php

namespace Photos;
use Engines\TestEngine;
class Photo {
    use \Engines\LikesEngineTrait;
//      ^^^^^^^^^^^^^^^^^^^^^^^^^
//      error: restricted to use Engines\LikesEngineTrait, it's not required by @photos
  public static function printPhotoLikes() {
    self::print();
      TestEngine::tester();
  }
}
