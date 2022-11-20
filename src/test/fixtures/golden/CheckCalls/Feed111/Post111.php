<?php /** @noinspection PhpUnusedAliasInspection */

namespace Feed111;

use Utils\Hidden;

/**
 * @see Hidden
 */
class Post111 {
  static function demo() {
    \Utils\Strings::<error descr="[modulite] restricted to call Utils\Strings::hidden1(), it's internal in @utils">hidden1</error>();
    <error descr="[modulite] restricted to use Utils\Hidden, it's internal in @utils">Hidden</error>::<error descr="[modulite] restricted to call Utils\Hidden::demo(), it's internal in @utils">demo</error>();
    self::forceHidden();
    plainPublic1();
//  ^^^^^^^^^^^^
//  error: restricted to call plainPublic1(), @plain is not required by @feed
    plainHidden1();
//  ^^^^^^^^^^^^
//  error: restricted to call plainHidden1(), @plain is not required by @feed
  }

  static function forceHidden() {
    if (0) {
      <error descr="[modulite] restricted to use Utils\Hidden, it's internal in @utils">Hidden</error>::<error descr="[modulite] restricted to call Utils\Hidden::demo(), it's internal in @utils">demo</error>();
    }
    if (0) {
      globalDemo();
//    ^^^^^^^^^^
//    error: restricted to call globalDemo(), it's not required by @feed
    }
    if (0) {
      <error descr="[modulite] restricted to use Messages111\Core111\Core111, @message/core is not required by @feed">\Messages111\Core111\Core111</error>::<error descr="[modulite] restricted to call Messages111\Core111\Core111::demo1(), @message/core is not required by @feed">demo1</error>();
    }
  }

  static function demoRequireUnexported() {
    require_once __DIR__ . '/../parent/child1/child2/child3/Child3.php';
    echo \Child3::child3Func();
    echo \Child3::hidden3Func();
  }
}
