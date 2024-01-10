<?php

namespace Logs100;

abstract class BaseLog100 {
  protected static function logAction(): bool {
    return true;
  }

  protected static function createLog(): void {
  }
}
