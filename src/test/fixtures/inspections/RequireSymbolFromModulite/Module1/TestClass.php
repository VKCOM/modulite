<?php

namespace Module1;

class TestClass {
	public function __construct() {
		var_dump(new \Module2\TestClass());
	}
}
