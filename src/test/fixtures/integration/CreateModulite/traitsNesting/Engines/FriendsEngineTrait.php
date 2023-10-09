<?php

namespace Engines;

trait FriendsEngineTrait {
    use VideosEngineTrait;
  public static function friendList(): void {
    echo "Your friends";
  }

  public static function printFriendList(): void {
   echo "friends";
  }
}
