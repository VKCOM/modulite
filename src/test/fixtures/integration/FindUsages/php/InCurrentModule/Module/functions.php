<?php

namespace Module;

define("FIND_USAGES_GLOBAL_DEFINE", 0);
const FIND_USAGES_GLOBAL_CONST = 0;
$FindUsagesGlobalVariable = 0;

class FindUsagesClass {
	const CONSTANT = 0;
	public static $staticField = 0;

	public static function staticMethod(): int { return 0; }
}

function find_usages_global_function() {
	echo 1;
}
