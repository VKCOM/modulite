<?php

namespace Messages\Core;

use Messages\OtherCore\OtherBaseClass;

class BaseClass {
}

class InternalBaseClass {
}

class BaseClassOther extends OtherBaseClass {
#                            ^^^^^^^^^^^^^^
#                            error: restricted to use Messages\OtherCore\OtherBaseClass, it's internal in @messages/other-core
}
