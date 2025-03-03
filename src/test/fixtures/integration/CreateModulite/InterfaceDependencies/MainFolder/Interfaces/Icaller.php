<?php

namespace Interfaces;

class Icaller
{
    public static function callInterfaceMethod(Task $task)
    {
        $task->getSerializedTaskDataJson();
    }
}
