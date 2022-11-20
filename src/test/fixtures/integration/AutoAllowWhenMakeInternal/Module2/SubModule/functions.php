<?php

namespace Module2\SubModule;

use BaseModule1\TestClass;
use BaseModule1\TestClassWithMethod;
use function BaseModule1\test_function;
use const BaseModule1\TEST_CONSTANT;

function foo() {
	var_dump(new TestClass());
  test_function();
  echo TEST_CONSTANT;
  echo TEST_DEFINE;
  TestClassWithMethod::test_method();
  echo TestClassWithMethod::$test_field;
  echo TestClassWithMethod::TEST_CONSTANT;
}
