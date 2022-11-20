<?php

namespace Feed;

class Post {
  static function demo() {
    global $somehow;
//         ^^^^^^^^
//         error: restricted to use global $somehow, it's not required by @feed
    Infra\Strings::demo();
  }
}
