<?php

class MainClass {
  public function main() {
    $_ = new Module\Inner\Foo();
    $_ = new Module\Depth2\Depth3\Depth3();
    $_ = new Module\Depth2\Depth3\HiddenDepth3();
//           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//           error: restricted to use Module\Depth2\Depth3\HiddenDepth3, it belongs to @module/depth2/hidden-depth3,
//                  which is internal in its parent modulite
  }
}
