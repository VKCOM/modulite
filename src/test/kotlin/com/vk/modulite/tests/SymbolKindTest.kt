package com.vk.modulite.tests

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.vk.modulite.SymbolName
import com.vk.modulite.SymbolName.Kind

class SymbolKindTest : BasePlatformTestCase() {
    fun `test different names`() {
        compareKind("@mod", Kind.Modulite)
        compareKind("@mod/sub", Kind.Modulite)
        compareKind("#vk/pack", Kind.ComposerPackage)

        compareKind("Foo", Kind.Class)
        compareKind("\\Foo", Kind.Class)
        compareKind("\\VK\\Messages\\Foo", Kind.Class)

        compareKind("Foo::\$field", Kind.Field)
        compareKind("\\Foo::\$field", Kind.Field)

        compareKind("Foo::method()", Kind.Method)
        compareKind("\\Foo::method()", Kind.Method)

        compareKind("Foo::CLASS_CONSTANT", Kind.ClassConstant)
        compareKind("\\Foo::CLASS_CONSTANT", Kind.ClassConstant)

        compareKind("foo()", Kind.Function)
        compareKind("\\foo()", Kind.Function)

        compareKind("\$GlobalVariable", Kind.GlobalVariable)
        compareKind("\\\$GlobalVariable", Kind.GlobalVariable)
        compareKind("\\\\\$GlobalVariable", Kind.GlobalVariable)

        compareKind("CONSTANT", Kind.Constant)
        compareKind("\\CONSTANT", Kind.Constant)
        compareKind("\\VK\\Message\\CONSTANT", Kind.Constant)
    }

    private fun compareKind(name: String, actual: Kind) {
        assertEquals(SymbolName(name).kind, actual)
    }
}
