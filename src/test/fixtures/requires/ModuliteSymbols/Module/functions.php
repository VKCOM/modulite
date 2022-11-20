<?php

namespace Module;

use GlobalClass;
use Module2\CommonGlobalClass;
use function Module2\common_global_function;
use const Module2\COMMON_GLOBAL_CONST;

function foo() {
	global $GlobalVariable;

	var_dump(new CommonGlobalClass());
	echo CommonGlobalClass::CONSTANT;
	echo CommonGlobalClass::staticMethod();
	echo CommonGlobalClass::$staticField;
	common_global_function();
	echo COMMON_GLOBAL_DEFINE;
	echo COMMON_GLOBAL_CONST;

	var_dump(new GlobalClass());
	echo GlobalClass::CONSTANT;
	echo GlobalClass::staticMethod();
	echo GlobalClass::$staticField;
	global_function();
	echo GLOBAL_CONST;
}
