<?php

namespace Users;

use Messages\ModuleClass;
use function Messages\module_function;
use const Messages\MODULE_CONST;

function foo() {
	global $MessagesGlobalVariable;

	var_dump(new ModuleClass());
	echo ModuleClass::CONSTANT;
	echo ModuleClass::staticMethod();
	echo ModuleClass::$staticField;
	module_function();
	echo MODULE_DEFINE;
	echo MODULE_CONST;
	echo $MessagesGlobalVariable;
}