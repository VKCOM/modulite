<?php

namespace Engines;

trait LikesEngineTrait {
    use FriendsEngineTrait;
    use MusicEngineTrait;
  public static function print(): void {
    echo "like";
  }

  public static function printTweetsLikes(): void {
    \ExternalEngines\Tweets::printTweetsLikesCountByUser();
  }
}
