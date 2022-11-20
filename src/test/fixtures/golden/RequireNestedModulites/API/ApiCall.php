<?php

namespace API;

class ApiCall {
    static function makeCall(string $api_method_name) {
        Impl\ApiInternals::doSmth();
    }
}
