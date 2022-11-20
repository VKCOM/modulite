<?php

namespace Messages;

class Messages {
    static function act() {
        Channels\Channels::doSmth();
        Core\Core::doSmth();
        \API\ApiCall::makeCall('');
    }
}
