<?php

namespace Messages;

use Messages\Core\BaseClass;
use Messages\Core\InternalBaseClass;

class ChildClass extends BaseClass {
}

class ChildClass2 extends InternalBaseClass {
#                         ^^^^^^^^^^^^^^^^^
#                         error: restricted to use Messages\Core\InternalBaseClass, it's internal in @messages/core
}
