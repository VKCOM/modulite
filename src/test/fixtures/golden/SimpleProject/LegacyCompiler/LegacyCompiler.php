<?php

namespace LegacyCompiler;

use Compiler\Lexer\Impl\FlexLexer;

class Compiler {
  // Хоть модуль @compiler/lexer не экспортирован из @compiler
  // за счет allow-internal-access в @compiler, мы можем
  // использовать этот класс.
  private FlexLexer $lexer;
}
