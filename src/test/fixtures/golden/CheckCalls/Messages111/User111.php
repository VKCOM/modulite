<?php /** @noinspection PhpUnusedAliasInspection */

namespace Messages111;

use Feed111\Post111;
use Utils\Hidden;
use Utils\Strings;

/**
 * @see Post111
 * @see Strings
 * @see Hidden
 */
class User111 {
  static function demo() {
    if (0) {
      <error descr="[modulite] restricted to use Feed111\Post111, @feed is not required by @message">Post111</error>::<error descr="[modulite] restricted to call Feed111\Post111::demo(), @feed is not required by @message">demo</error>();
    }
    if (0) {
      <error descr="[modulite] restricted to use Utils\Strings, @utils is not required by @message">Strings</error>::<error descr="[modulite] restricted to call Utils\Strings::normal(), @utils is not required by @message">normal</error>();
    }
    if (0) {
      <error descr="[modulite] restricted to use Utils\Hidden, @utils is not required by @message">Hidden</error>::<error descr="[modulite] restricted to call Utils\Hidden::demo2(), @utils is not required by @message">demo2</error>();
    }
  }
}
