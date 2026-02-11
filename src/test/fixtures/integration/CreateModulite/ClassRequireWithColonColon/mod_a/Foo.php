<?php
namespace mod_a;

use mod_b\Bar;

final class Foo {

  public static function work(): void {
    echo Bar::class;
   // new Bar();
  }
}
