package com.vk.modulite.tests

import com.vk.modulite.infrastructure.ConfigListTestBase

class ExportListSortTest : ConfigListTestBase() {
    fun `test add function`() = doExportTest(
        "\\foo()",
        listOf(
            "@mod",
            "Foo",
            "Foo::method()",
        ),
        listOf(
            "@mod",
            "Foo",
            "Foo::method()",
            "foo()",
        ),
    )

    fun `test add to unsorted list`() = doExportTest(
        "\\foo()",
        listOf(
            "Foo::\$field",
            "@mod",
            "\$Global",
            "Foo",
            "CONSTANT",
            "Foo::CONSTANT",
            "#vk/pack",
            "#vk/pack2",
            "VK\\Message\\CONSTANT",
            "\$Global2",
            "Foo::method()",
        ),
        listOf(
            "@mod",
            "#vk/pack",
            "#vk/pack2",
            "Foo",
            "Foo::method()",
            "Foo::\$field",
            "Foo::CONSTANT",
            "foo()",
            "CONSTANT",
            "VK\\Message\\CONSTANT",
            "\$Global",
            "\$Global2",
        ),
    )

    fun `test add to sorted list`() = doExportTest(
        "\\foo()",
        listOf(
            "@mod",
            "#vk/pack",
            "#vk/pack2",
            "Foo",
            "Foo::\$field",
            "Foo::CONSTANT",
            "CONSTANT",
            "VK\\Message\\CONSTANT",
            "\$Global",
            "\$Global2",
        ),
        listOf(
            "@mod",
            "#vk/pack",
            "#vk/pack2",
            "Foo",
            "Foo::\$field",
            "Foo::CONSTANT",
            "foo()",
            "CONSTANT",
            "VK\\Message\\CONSTANT",
            "\$Global",
            "\$Global2",
        ),
    )

    fun `test add to sorted list 2`() = doExportTest(
        "CONSTANT",
        listOf(
            "@mod",
            "#vk/pack",
            "#vk/pack2",
            "Foo",
            "Foo::\$field",
            "Foo::CONSTANT",
            "foo()",
            "VK\\Message\\CONSTANT",
            "\$Global",
            "\$Global2",
        ),
        listOf(
            "@mod",
            "#vk/pack",
            "#vk/pack2",
            "Foo",
            "Foo::\$field",
            "Foo::CONSTANT",
            "foo()",
            "CONSTANT",
            "VK\\Message\\CONSTANT",
            "\$Global",
            "\$Global2",
        ),
    )

    // Add method do nothing for export list
    fun `test add method`() = doExportTest(
        "Foo::method()",
        listOf(),
        listOf(),
    )

    // Add field do nothing for export list
    fun `test add field`() = doExportTest(
        "Foo::\$field",
        listOf(),
        listOf(),
    )

    // Add class constant do nothing for export list
    fun `test add class constant`() = doExportTest(
        "Foo::CONSTANT",
        listOf(),
        listOf(),
    )

    // Add global variable do nothing for export list
    fun `test add global`() = doExportTest(
        "\\\$GlobalVariable",
        listOf(),
        listOf(),
    )
}
