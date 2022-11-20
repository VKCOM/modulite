<?php

use Compiler\Lexer\Impl\FlexLexer;

function main() {
  echo FlexLexer::$stateFlags; // ok
  FlexLexer::process(); // ok
  FlexLexer::traitProcess(); // ok
}
