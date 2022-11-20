<?php

namespace VK\User;

//$a = new \VK\Common\Root();
$a = new \VK\Common\Time\Time();

$a = new \VK\Common\Time\Impl\TimeImpl();
//       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//       error: restricted to use VK\Common\Time\Impl\TimeImpl, it belongs to @time/impl,
//              which is internal in its parent modulite

$a = new \VK\Common\Time\Impl\AllowedTimeImpl();
