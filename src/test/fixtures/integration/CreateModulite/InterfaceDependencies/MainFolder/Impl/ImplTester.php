<?php

namespace Impl;

use Interfaces\Task;

final class ImplTester implements Task {
  public function getSerializedTaskDataJson(): string {
    return "";
  }
}
