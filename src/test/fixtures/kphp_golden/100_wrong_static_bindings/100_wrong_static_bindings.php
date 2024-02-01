@kphp_should_fail
KPHP_ENABLE_MODULITE=1
/restricted to call Logs100\\BaseLog100:createLog\(\), it's not required by @messages100/
/restricted to call Logs100\\BaseLog100::logAction\(\), it's not required by @messages100/
<?php
#ifndef KPHP
require_once 'kphp_tester_include.php';
#endif

$_ = \Messages100\MessagesLogger100::log();

\Messages100\MessagesLogger100::create();
