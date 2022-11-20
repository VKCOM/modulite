<?php

$a = new \VK\Common\Time();
$b = new \VK\Curl\Executor();
//       ^^^^^^^^^^^^^^^^^
//       error: restricted to use VK\Curl\Executor, #vk/curl it not required by @user
