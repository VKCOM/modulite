<?php

const GLOBAL_CONST = 0;

class GlobalClass {
	const CONSTANT = 0;

	public static function staticMethod() {}
	public static $staticField = 0;
}

function global_function() {
	echo 1;
}
