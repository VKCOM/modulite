<?php

namespace VK\Messages;

require_once "../User/functions.php";

use VK\User\User;

class Message {
	public function __construct() {
    // class scope
    global $GlobalVariable;

    $GlobalLambda = function() { echo 1; };
    $GlobalLambda();

//    $GlobalLambda();

	  echo TEST_DEFINE;
    // KPHP not support constants like this
	  // echo \VK\User\TEST_CONST;
    echo $GlobalVariable;
//    echo \VK\User\test();
    var_dump(new User());
	}
}
