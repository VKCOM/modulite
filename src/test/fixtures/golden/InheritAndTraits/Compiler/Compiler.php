<?php

namespace Compiler;

use Compiler\Lexer\Impl\FlexLexer;
use Compiler\Lexer\Lexer;

class Compiler {
  private Lexer $lexer;

  public function compile(string $filename) {
    echo FlexLexer::$stateFlags; // ok
    FlexLexer::process(); // ok
    FlexLexer::traitProcess(); // ok
  }
}
