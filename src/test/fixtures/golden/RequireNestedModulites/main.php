<?php

Messages\Messages::act();

require_once __DIR__ . '/parent/ParentFuncs.php';
ParentFuncs::testThatCantAccessSubsubchild();
