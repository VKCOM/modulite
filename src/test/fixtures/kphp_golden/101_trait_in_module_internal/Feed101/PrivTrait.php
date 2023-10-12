<?php

namespace Feed101;

trait PrivTrait {
    function showThisClass() {
        echo get_class($this), "\n";
    }
}
