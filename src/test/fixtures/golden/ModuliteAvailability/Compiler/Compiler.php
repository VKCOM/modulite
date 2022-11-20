<?php

namespace Compiler;

use Compiler\Lexer\Impl\FlexLexer;
use Compiler\Lexer\Impl2\InternalImplFlexLexer;
use Compiler\Lexer\Lexer;

class Compiler {
  private Lexer $lexer;

  public function compile(string $filename) {
    // Здесь мы можем использовать FlexLexer, так как в текущем модуле мы можем
    // получить доступ к модулю @compiler1/lexer/impl, так как @compiler1/lexer
    // является прямым потомком текущего модуля, а значит мы может обращаться
    // ко всем его символам включая экспортированный @compiler1/lexer/impl.
    $lexer = new FlexLexer();
    $lexer->lex($filename);

    // Мы не можем использовать InternalImplFlexLexer, так как @compiler1/lexer/impl2
    // не жкспортирован из @compiler1/lexer.
    $lexer = new InternalImplFlexLexer();
    //           ^^^^^^^^^^^^^^^^^^^^^
    //           error: restricted to use Compiler\Lexer\Impl2\InternalImplFlexLexer, it belongs to @compiler1/lexer/impl2,
    //                  which is internal in its parent modulite
  }
}
