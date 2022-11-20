<?php

namespace Module\SubModule;

use Module\FindUsagesClass;
use function Module\find_usages_global_function;
use const Module\FIND_USAGES_GLOBAL_CONST;

function main() {
	global $FindUsagesGlobalVariable;

	var_dump(new FindUsagesClass());
	echo FindUsagesClass::CONSTANT;
	echo FindUsagesClass::staticMethod();
	echo FindUsagesClass::$staticField;
	find_usages_global_function();
	echo FIND_USAGES_GLOBAL_DEFINE;
	echo FIND_USAGES_GLOBAL_CONST;
	echo $FindUsagesGlobalVariable;
}
