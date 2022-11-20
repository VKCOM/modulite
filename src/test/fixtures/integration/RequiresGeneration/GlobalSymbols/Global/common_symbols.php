<?php

namespace Common;

define("COMMON_GLOBAL_DEFINE", 0);
const COMMON_GLOBAL_CONST = 0;
$GlobalVariable = 0;

class CommonGlobalClass {
	const CONSTANT = 0;

	public static function staticMethod() {}
	public static $staticField = 0;
}

function common_global_function() {
	echo 1;
}
