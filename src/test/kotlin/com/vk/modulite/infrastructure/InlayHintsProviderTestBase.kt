package com.vk.modulite.infrastructure

import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase
import com.vk.modulite.highlighting.hints.PhpInlayTypeHintsProvider
import java.io.File

open class InlayHintsProviderTestBase : InlayHintsProviderTestCase() {
    protected fun runHintTest(testFile: String) {
        val testDataFolder = File(testDataPath, "hints")
        assertTrue(testDataFolder.exists())

        val walker = testDataFolder.walk()
        val files = walker
            .filter { it.isFile && (it.extension == "php" || it.extension == "yaml" || it.extension == "qf") }
            .map { it.path.removePrefix(testDataPath) }
            .toList().toTypedArray()

        myFixture.configureByFiles(*files)

        val file = File("src/test/fixtures/hints/$testFile.expected")
        val expectedText = file.readText()

        doTestProvider("functions.php", expectedText, PhpInlayTypeHintsProvider())
    }

    override fun getTestDataPath() = "src/test/fixtures"
}
