<?php

namespace Module;

define("MODULE_GLOBAL_DEFINE", 0);
const MODULE_GLOBAL_CONST = 0;
$ModuleGlobalVariable = 0;

class ModuleGlobalClass {
	const CONSTANT = 0;

	public static function staticMethod() {}
	public static $staticField = 0;
}

function module_global_function() {
	echo 1;
}
