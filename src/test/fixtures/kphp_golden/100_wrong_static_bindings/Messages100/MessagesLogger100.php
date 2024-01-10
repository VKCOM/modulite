<?php

namespace Messages100;

use Logs100\BaseLog100;

class MessagesLogger100 extends BaseLog100 {

  public static function create(): void {
    parent::createLog();
//          ^^^^^^^^^
//          error: restricted to call Logs100\BaseLog100::createLog(), it's not required by @messages100
  }

  public static function log(): bool {
    return parent::logAction();
//                 ^^^^^^^^^
//                 error: restricted to call Logs100\BaseLog100::logAction(), it's not required by @messages100
  }
}
