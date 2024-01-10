<?php

namespace Photos;

class Photo {
  use \Engines\LikesEngineTrait;

  public static function printPhotoLikes() {
    self::print();
  }
}
