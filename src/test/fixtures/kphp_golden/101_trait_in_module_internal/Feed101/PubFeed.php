<?php

namespace Feed101;

class PubFeed {
    /**
     * @param $cb callable(PrivClass):void
     */
    static function accessToPrivClass(callable $cb) {
        $impl = new PrivClass;
        $cb($impl);
    }
}
