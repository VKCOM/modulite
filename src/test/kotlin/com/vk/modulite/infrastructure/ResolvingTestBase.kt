package com.vk.modulite.infrastructure

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.refactoring.suggested.endOffset
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.vk.modulite.utils.YamlUtils
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YamlRecursivePsiElementVisitor
import java.io.File

abstract class ResolvingTestBase : BasePlatformTestCase() {
    companion object {
        private const val TEST_DATA_PATH = "src/test/fixtures/"
    }

    override fun getTestDataPath() = "src/test/fixtures"

    protected fun runFixture(dir: String) {
        LocalFileSystem.getInstance().refresh(false)

        val genTestDataFolder = File(TEST_DATA_PATH + dir)
        assertTrue(genTestDataFolder.exists())

        val walker = genTestDataFolder.walk()
        val files = walker
            .filter { it.isFile && (it.extension == "php" || it.extension == "yaml") }
            .map { it.path.removePrefix(TEST_DATA_PATH) }
            .toList().toTypedArray()

        myFixture.configureByFiles(*files)

        files.forEach {
            if (!it.endsWith(".yaml")) {
                return@forEach
            }

            val file = myFixture.findFileInTempDir(it) ?: return@forEach
            myFixture.openFileInEditor(file)

            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@forEach

            var countQuotedText = 0
            var resolvedReferences = 0

            psiFile.accept(object : YamlRecursivePsiElementVisitor() {
                override fun visitQuotedText(quotedText: YAMLQuotedText) {
                    val ref = psiFile.findReferenceAt(quotedText.endOffset - 1)
                    if (ref == null) {
                        if (YamlUtils.insideNamespace(quotedText) || YamlUtils.insideDescription(quotedText)) {
                            return
                        }

                        countQuotedText++
                        return
                    }
                    val resolvedElement = ref.resolve()
                    assertNotNull("can't resolve ${ref.element.text} reference", resolvedElement)

                    resolvedReferences++
                    countQuotedText++
                }
            })

            println("${file.path}: Resolved $resolvedReferences references from $countQuotedText quoted texts")
            assertEquals(resolvedReferences, countQuotedText)
        }
    }
}
