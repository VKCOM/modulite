<?php

namespace Module;

use Common\CommonGlobalClass;
use GlobalClass;
use function Common\common_global_function;
use const Common\COMMON_GLOBAL_CONST;

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
