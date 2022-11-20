<?php

const OTHER_CONST = 112;

function other() {
  echo DEF_POST_PUB, "\n";
//     ^^^^^^^^^^^^
//     error: restricted to use DEF_POST_PUB, @feed is not required by @other
  echo GLOBAL_DEF, "\n";
//     ^^^^^^^^^^
//     error: restricted to use GLOBAL_DEF, it's not required by @other
}
