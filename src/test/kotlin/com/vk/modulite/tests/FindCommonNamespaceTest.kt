package com.vk.modulite.tests

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName.Kind
import com.vk.modulite.services.ModuliteDependenciesCollector
import io.kotest.matchers.shouldBe

class FindCommonNamespaceTest : BasePlatformTestCase() {
    fun `test find common namespace`() {
        common("A", "B")                shouldBe "<global namespace>"
        common("A", "A")                shouldBe "A"
        common("A", "A\\B")             shouldBe "A"
        common("A\\B\\C", "A\\B\\D")    shouldBe "A\\B"
        common("A\\B\\C", "A\\B\\C")    shouldBe "A\\B\\C"
        common("A\\B\\C", "A\\B\\C\\D") shouldBe "A\\B\\C"
    }

    private fun common(vararg namespaces: String): String {
        val nss = namespaces.toList().map { Namespace(it) }
        return ModuliteDependenciesCollector.getInstance(project).findCommonNamespace(nss).toPHP()
    }
}
