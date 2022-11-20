<?php

use Compiler\Lexer\Impl\FlexLexer;

function main() {
  // Несмотря на то, что FlexLexer экспортируется из модуля @compiler1/lexer/impl
  // он не может быть использован из глобального кода, так как модуль @compiler1/lexer
  // не экспортируется. Ввиду этого глобальный код не может получить доступа к FlexLexer.

  $lexer = new FlexLexer();
  //           ^^^^^^^^^
  //           error: restricted to use Compiler\Lexer\Impl\FlexLexer, it belongs to @compiler1/lexer/impl,
  //                  which is internal in its parent modulite
}
