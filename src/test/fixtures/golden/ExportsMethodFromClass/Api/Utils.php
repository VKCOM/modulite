<?php

namespace Api;

use Core\Logger;

class Utils {
  public function log() {
    Logger::debug();

    if (0) {
      <error descr="[modulite] restricted to use Core\Logger, it's internal in @core">Logger</error>::<error descr="[modulite] restricted to call Core\Logger::log(), it's internal in @core">log</error>();
    }
  }
}
