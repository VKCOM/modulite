<?php

namespace Utils110;

interface I110 extends \Feed110\IRank110 {
//                     ^^^^^^^^^^^^^^^^^
//                     error: restricted to use Feed110\IRank110, it's internal in @feed
}

class More110 implements \Feed110\IPost110 {
//                       ^^^^^^^^^^^^^^^^^
//                       error: restricted to use Feed110\IPost110, it's internal in @feed
}
