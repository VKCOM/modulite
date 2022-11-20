package com.vk.modulite.tests

import com.vk.modulite.infrastructure.ConfigListTestBase

class RequiresListTest : ConfigListTestBase() {
    // Перед вставкой будет произведена сортировка
    fun `test add to unsorted list`() = doRequiresTest(
        "\\foo()",
        listOf(
            "\\Foo::\$field",
            "@mod",
            "\$Global",
            "\\Foo",
            "\\CONSTANT",
            "\\Foo::CONSTANT",
            "#vk/pack",
            "#vk/pack2",
            "\\VK\\Message\\CONSTANT",
            "\$Global2",
            "\\Foo::method()",
        ),
        listOf(
            "@mod",
            "#vk/pack",
            "#vk/pack2",
            "\\Foo",
            "\\Foo::method()",
            "\\Foo::\$field",
            "\\Foo::CONSTANT",
            "\\foo()",
            "\\CONSTANT",
            "\\VK\\Message\\CONSTANT",
            "\$Global",
            "\$Global2",
        ),
    )
}