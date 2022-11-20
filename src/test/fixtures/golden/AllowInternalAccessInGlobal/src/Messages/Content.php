<?php

namespace VK\Messages;

use VK\User\User;

class Content {
  public function method() {
    // method scope
    global $GlobalVariable;
    echo TEST_DEFINE;
    // KPHP not support constants like this
    // echo \VK\User\TEST_CONST;
    echo $GlobalVariable;
//    echo \VK\User\test();
    var_dump(new User());
  }
}
