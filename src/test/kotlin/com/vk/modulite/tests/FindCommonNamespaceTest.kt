package com.vk.modulite.tests

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.vk.modulite.Namespace
import com.vk.modulite.services.ModuliteDependenciesCollector

class FindCommonNamespaceTest : BasePlatformTestCase() {
    fun `test find common namespace`() {
        common("A", "B")                beEqual "<global namespace>"
        common("A", "A")                beEqual "A"
        common("A", "A\\B")             beEqual "A"
        common("A\\B\\C", "A\\B\\D")    beEqual "A\\B"
        common("A\\B\\C", "A\\B\\C")    beEqual "A\\B\\C"
        common("A\\B\\C", "A\\B\\C\\D") beEqual "A\\B\\C"
    }

    private fun common(vararg namespaces: String): String {
        val nss = namespaces.toList().map { Namespace(it) }
        return ModuliteDependenciesCollector.getInstance(project).findCommonNamespace(nss).toPHP()
    }

    private infix fun <T, U: T> T.beEqual(expect: U?) {
        assertEquals(expect, this)
    }
}
