<?php /** @noinspection PhpDefineCanBeReplacedWithConstInspection */

namespace VK\Monolith;

define("TEST_MONOLITH_DEFINE", 0);
const TEST_CONST = 0;

$GlobalMonolithVariable = 0;

function test() {}

class Test {
  const CONSTANT = 0;

  public static $staticField = 0;
  public static function staticMethod() {}
}
