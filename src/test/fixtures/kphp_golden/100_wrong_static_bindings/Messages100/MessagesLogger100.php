<?php

namespace Messages100;

use Logs100\BaseLog100;

class MessagesLogger100 extends BaseLog100 {

  public static function create(): void {
    parent::createLog();
  }

  public static function log(): bool {
    return parent::logAction();
  }
}
