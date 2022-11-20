<?php

namespace Messages;

define("MODULE_DEFINE", 0);
const MODULE_CONST = 0;
$MessagesGlobalVariable = 0;

class ModuleClass {
	const CONSTANT = 0;

	public static function staticMethod(): int { return 1; }
	public static $staticField = 0;
}

function module_function() {
	echo 1;
}
