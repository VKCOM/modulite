<?php

namespace Photos101;

class Photo101 {
  use \Engines101\LikesEngineTrait101;
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//error: restricted to use Engines101\LikesEngineTrait101, it's not required by @photos
  public static function printPhotoLikes() {
    self::print();
  }
}
