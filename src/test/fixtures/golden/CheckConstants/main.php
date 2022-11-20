<?php

\Feed\Post::demo();
<error descr="[modulite] restricted to use Feed\Infra\Strings, it belongs to @feed/infra,
which is internal in its parent modulite">\Feed\Infra\Strings</error>::<error descr="[modulite] restricted to call Feed\Infra\Strings::demo(), it belongs to @feed/infra,
which is internal in its parent modulite">demo</error>();

echo <error descr="[modulite] restricted to use Feed\Infra\Hidden::HIDDEN_2, it's internal in @feed/infra"><error descr="[modulite] restricted to use Feed\Infra\Hidden, it's internal in @feed/infra">Feed\Infra\Hidden</error>::HIDDEN_2</error>;


define('GLOBAL_DEF', 1);

require_once 'other/other.php';
other();

echo DEF_POST_PUB, "\n";
echo OTHER_CONST, "\n";
//   ^^^^^^^^^^^
//   error: restricted to use OTHER_CONST, it's internal in @other
