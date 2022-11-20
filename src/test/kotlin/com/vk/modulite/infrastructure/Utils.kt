package com.vk.modulite.infrastructure

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpReference
import com.vk.modulite.utils.unquote
import org.jetbrains.yaml.psi.YAMLQuotedText

object Utils {
    fun CodeInsightTestFixture.openFile(file: String): VirtualFile {
        val checkedFile = findFileInTempDir(file)
        openFileInEditor(checkedFile)
        return checkedFile
    }

    fun PsiFile.findElementByName(name: String): PhpNamedElement? {
        var element: PhpNamedElement? = null
        PsiTreeUtil.processElements(this) {
            if (it is PhpNamedElement && it.name == name) {
                element = it
                return@processElements false
            }

            true
        }

        return element
    }

    fun PsiFile.findReferenceByName(name: String): PhpReference? {
        var element: PhpReference? = null
        PsiTreeUtil.processElements(this) {
            if (it is PhpReference && it.name == name) {
                element = it
                return@processElements false
            }

            true
        }

        return element
    }

    fun PsiFile.findQuotedTextByValue(name: String): YAMLQuotedText? {
        var element: YAMLQuotedText? = null
        PsiTreeUtil.processElements(this) {
            if (it is YAMLQuotedText && it.text.unquote() == name) {
                element = it
                return@processElements false
            }

            true
        }

        return element
    }

    fun CodeInsightTestFixture.checkHighlighting(files: Array<String>) {
        checkHighlighting(files.toList())
    }

    fun CodeInsightTestFixture.checkHighlighting(files: List<String>) {
        for (it in files) {
            val checkedFile = findFileInTempDir(it) ?: continue
            openFileInEditor(checkedFile)
            checkHighlighting()
        }
    }

    fun waitWithEventsDispatching(timeoutInSeconds: Int) {
        val start = System.currentTimeMillis()
        while (true) {
            try {
                if (System.currentTimeMillis() - start > timeoutInSeconds * 1000L) {
                    break
                }
                PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }
    }
}
