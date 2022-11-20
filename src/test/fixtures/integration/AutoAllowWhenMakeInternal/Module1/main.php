<?php

namespace BaseModule1;

function main() {
  var_dump(new TestClass());
  test_function();
  echo TEST_CONSTANT;
  echo TEST_DEFINE;
  TestClassWithMethod::test_method();
  echo TestClassWithMethod::$test_field;
  echo TestClassWithMethod::TEST_CONSTANT;
}
