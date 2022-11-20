package com.vk.modulite.tests

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

@Suppress("DEPRECATION")
class DocumentationTest : BasePlatformTestCase() {
    override fun getTestDataPath() = "src/test/fixtures/docs"

    fun `test modulite hover and ctrl click doc`() {
        myFixture.configureByFiles("Module/.modulite.yaml")
        val originalElement = myFixture.elementAtCaret
        var element = DocumentationManager
            .getInstance(project)
            .findTargetElement(myFixture.editor, originalElement.containingFile, originalElement)

        if (element == null) {
            element = originalElement
        }

        val documentationProvider = DocumentationManager.getProviderFromElement(element)
        checkFullDoc(documentationProvider, element, originalElement, "$testDataPath/Doc.expected.html")
        checkQuickNavigateDoc(documentationProvider, element, originalElement, "$testDataPath/QuickNavigateDoc.expected.html")
    }

    private fun checkFullDoc(
        documentationProvider: DocumentationProvider,
        element: PsiElement,
        originalElement: PsiElement,
        fileToCheck: String,
    ) {
        val generateDoc = documentationProvider.generateDoc(element, originalElement)
        assertNotNull(generateDoc)

        checkDoc(generateDoc!!, fileToCheck)
    }

    private fun checkQuickNavigateDoc(
        documentationProvider: DocumentationProvider,
        element: PsiElement,
        originalElement: PsiElement,
        fileToCheck: String,
    ) {
        val generateDoc = documentationProvider.getQuickNavigateInfo(element, originalElement)
        assertNotNull(generateDoc)

        checkDoc(generateDoc!!, fileToCheck)
    }

    private fun checkDoc(generateDoc: String, fileToCheck: String) {
        val file = File(fileToCheck)
        if (!file.exists()) {
            file.createNewFile()
            file.writeText(generateDoc)
            error("File $fileToCheck not found. Generated doc was written to $file")
        }

        val text = file.readText()
        assertEquals(text, generateDoc)
    }
}
