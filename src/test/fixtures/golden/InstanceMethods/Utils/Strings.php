<?php

namespace Utils;

class Strings {
    public int $count = 0;

    public function strlen(string $s) {
        return strlen($s);
    }

    static public function demoAccessHidden() {
        // this is Hidden — not exported
        // but we don't check calling instance methods and accessing instance properties
        // so, that is okay
        // BTW, this is not okay: /** @var \Messages\Hidden $hidden */
        // because we can't access that class
        // as a consequence, we can't accept it as a parameter
        // (in practice, that class should be exported, logically)
        $hidden = \Messages\Messages::getHidden();
        $hidden->incId();
        echo $hidden->id, "\n";

        // that's also ok, we don't check instance methods
        $msg = new \Messages\Messages;
        $msg->instanceFnMarkedForceInternal();
    }
}
