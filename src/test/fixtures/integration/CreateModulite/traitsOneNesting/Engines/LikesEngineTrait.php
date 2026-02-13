<?php

namespace Engines;

trait LikesEngineTrait {
  public static function print(): void {
    echo "like";
  }

  public static function printTweetsLikes(): void {
    \ExternalEngines\Tweets::printTweetsLikesCountByUser();
  }
}
