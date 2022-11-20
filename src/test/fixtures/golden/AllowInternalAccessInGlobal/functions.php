<?php

use VK\Messages\Content;
use VK\Messages\Message;
use VK\User\User;

function do_any() {
  // function scope
  global $GlobalVariable;

  echo TEST_DEFINE;
  // KPHP not support constants like this
  // echo \VK\User\TEST_CONST;
  echo $GlobalVariable;
//  echo \VK\User\test();
  var_dump(new User());

  $content = new Content();
  $content->method();
  $mess = new Message();
  var_dump($mess);
}
