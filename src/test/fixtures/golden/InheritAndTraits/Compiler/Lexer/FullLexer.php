<?php

namespace Compiler\Lexer;

use Compiler\Lexer\Impl\ExportLexerTraitWithInternalMethod;

class FullLexer {
  use ExportLexerTraitWithInternalMethod;

  public function lex(string $filename) {
    self::traitProcess();
  }
}
