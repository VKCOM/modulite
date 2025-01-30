package com.vk.modulite.infrastructure

import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.php.lang.psi.PhpPsiElementFactory
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.vk.modulite.SymbolName
import com.vk.modulite.psi.extensions.yaml.addDependencies
import com.vk.modulite.psi.extensions.yaml.makeElementExport
import com.vk.modulite.psi.extensions.yaml.relatedModulite
import com.vk.modulite.services.ModuliteDeps
import com.vk.modulite.utils.childOfType
import org.jetbrains.yaml.psi.YAMLFile

open class ConfigListTestBase : BasePlatformTestCase() {
    protected fun doExportTest(symbol: String, before: List<String>, after: List<String>) =
        doListTest("export", symbol, before, after)

    protected fun doRequiresTest(symbol: String, before: List<String>, after: List<String>) =
        doListTest("require", symbol, before, after)

    private fun doListTest(listName: String, symbol: String, before: List<String>, after: List<String>) {
        val beforeList = before.joinToString("\n") { "  - \"${it.toYaml()}\"" }
        val afterList = after.joinToString("\n") { "  - \"${it.toYaml()}\"" }

        val testFile = myFixture.configureByText(
            ".modulite.yaml",
            """
                |name: "@test"
                |namespace: "\\"
                |$listName:
                |$beforeList
            """.trimMargin()
        ) as YAMLFile

        val name = SymbolName(symbol)
        if (listName == "export") {
            val element = createElementByName(myFixture.project, name)
            testFile.makeElementExport(element)
        } else if (listName == "require") {
            testFile.addDependencies(ModuliteDeps(project, testFile.relatedModulite(), name))
        }

        val expected = """
            |name: "@test"
            |namespace: "\\"
            |$listName:
            |$afterList
        """.trimMargin()

        assertEquals(expected, testFile.text)
    }

    companion object {
        fun createElementByName(project: Project, name: SymbolName): PhpNamedElement {
            val symbolsName = name.name.removePrefix("\\")
            when (name.kind) {
                SymbolName.Kind.Class -> {
                    val file = PhpPsiElementFactory.createPsiFileFromText(project, "class $symbolsName {}")
                    return file.childOfType<PhpClass>() ?: error("Could not create class")
                }

                SymbolName.Kind.Field -> {
                    val (className, fieldName) = name.splitMember()
                    val file = PhpPsiElementFactory.createPsiFileFromText(project, "class  $className { public $fieldName; }")
                    return file.childOfType<Field>() ?: error("Could not create field")
                }

                SymbolName.Kind.Method -> {
                    val (className, methodName) = name.splitMember()
                    val file = PhpPsiElementFactory.createPsiFileFromText(project, "class $className { function $methodName {} }")
                    return file.childOfType<Function>() ?: error("Could not create method")
                }

                SymbolName.Kind.ClassConstant -> {
                    val (className, constName) = name.splitMember()
                    val file = PhpPsiElementFactory.createPsiFileFromText(project, "class $className { const $constName = 0; }")
                    return file.childOfType<Field>() ?: error("Could not create class constant")
                }

                SymbolName.Kind.Function -> {
                    val file = PhpPsiElementFactory.createPsiFileFromText(project, "function $symbolsName {}")
                    return file.childOfType<Function>() ?: error("Could not create function")
                }

                SymbolName.Kind.GlobalVariable -> {
                    val file = PhpPsiElementFactory.createPsiFileFromText(project, "$symbolsName = 1;")
                    return file.childOfType<Variable>() ?: error("Could not create global variable")
                }

                SymbolName.Kind.Constant -> {
                    val file = PhpPsiElementFactory.createPsiFileFromText(project, "const $symbolsName = 1;")
                    return file.childOfType<Constant>() ?: error("Could not create constant")
                }

                else -> {}
            }

            error("Could not create element")
        }

        fun String.toYaml() = replace("\\", "\\\\")
    }
}