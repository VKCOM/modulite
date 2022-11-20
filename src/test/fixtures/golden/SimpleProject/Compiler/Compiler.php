<?php

namespace Compiler;

use Compiler\Lexer\Impl\FlexLexer;
use Compiler\Lexer\Lexer;

class Compiler {
  private Lexer $lexer;

  public function compile(string $filename) {
    $lexer = new FlexLexer($filename);
    $lexer->lex($filename);
  }
}
