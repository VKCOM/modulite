<?php

namespace Compiler\Lexer;

interface Lexer {
  public function lex(string $filename);
}
