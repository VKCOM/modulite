<?php

namespace VK\Symbols\Impl;

class Helpers {
  static function getFirstSymbol(): ?string {
    $symbols = Symbols::SYMBOLS_LIST;
    return count($symbols) != 0 ? $symbols[0] : null;
  }
}
