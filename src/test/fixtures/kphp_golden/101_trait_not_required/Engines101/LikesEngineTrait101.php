<?php

namespace Engines101;

trait LikesEngineTrait101 {
  public static function print(): void {
    echo "like";
  }

  public static function printTweetsLikes(): void {
    \ExternalEngines101\Tweets101::printTweetsLikesCountByUser();
    $_ = new \ExternalEngines101\Tweets101();
  }
}
