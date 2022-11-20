<?php

namespace Feed;

define('DEF_POST_1', 1);
define('DEF_POST_2', 2);
define('DEF_POST_PUB', 3);

class Post {
  const ONE   = 1;
  const TWO   = 2;
  const THREE = 3;

  static function demo() {
    echo self::ONE, self::TWO, "\n";
    echo Infra\Strings::STR_NAME, "\n";
    echo <error descr="[modulite] restricted to use Feed\Infra\Strings::STR_HIDDEN, it's internal in @feed/infra">Infra\Strings::STR_HIDDEN</error>, "\n";
    echo <error descr="[modulite] restricted to use Feed\Infra\Hidden::HIDDEN_1, it's internal in @feed/infra"><error descr="[modulite] restricted to use Feed\Infra\Hidden, it's internal in @feed/infra">Infra\Hidden</error>::HIDDEN_1</error>, "\n";
    echo DEF_POST_1, DEF_STRINGS_1, DEF_STRINGS_2, "\n";
  }
}
