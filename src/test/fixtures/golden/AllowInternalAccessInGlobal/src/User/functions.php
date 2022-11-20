<?php /** @noinspection PhpDefineCanBeReplacedWithConstInspection */

namespace VK\User;

define("TEST_DEFINE", 0);
const TEST_CONST = 0;

$GlobalVariable = 0;

function test() {
  $GlobalLambda = function() { echo 1; };
  $GlobalLambda();
  return 0;
}
