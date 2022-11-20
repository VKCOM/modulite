package com.vk.modulite.dsl

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.suggested.startOffset
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.usages.Usage
import com.intellij.usages.UsageView
import com.intellij.usages.UsageViewManager
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpNamespace
import com.jetbrains.php.lang.psi.elements.Variable
import com.vk.modulite.Namespace
import com.vk.modulite.SymbolName
import com.vk.modulite.actions.ModuliteBuilder
import com.vk.modulite.actions.ModuliteBuilder.ModuliteData
import com.vk.modulite.actions.dialogs.ModuliteBuilderData
import com.vk.modulite.actions.usages.php.PhpUsagesFinder
import com.vk.modulite.actions.usages.yaml.YamlUsagesFinder
import com.vk.modulite.infrastructure.Utils
import com.vk.modulite.infrastructure.Utils.checkHighlighting
import com.vk.modulite.infrastructure.Utils.findElementByName
import com.vk.modulite.infrastructure.Utils.findQuotedTextByValue
import com.vk.modulite.infrastructure.Utils.findReferenceByName
import com.vk.modulite.infrastructure.Utils.waitWithEventsDispatching
import com.vk.modulite.inspections.InternalSymbolUsageInspection
import com.vk.modulite.inspections.intentions.AllowInternalAccessEmptyInspection
import com.vk.modulite.inspections.intentions.ChangeVisibilityEmptyInspection
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.modulite.ModuliteDependenciesManager
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.files.psiFile
import com.vk.modulite.psi.extensions.yaml.*
import com.vk.modulite.services.ModuliteIndex
import com.vk.modulite.utils.YamlUtils.getTopLevelKey
import junit.framework.TestCase.*
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YAMLSequence
import org.junit.Assert
import java.io.File
import java.util.concurrent.TimeUnit

@DslMarker
annotation class TestDslMarker

@TestDslMarker
class FindUsagesContext(val inModule: String, val element: PhpNamedElement, val usages: Collection<Usage>, val ctx: ElementContextBase) {
    fun dump(file: String) {
        val filename = generateFilename(file)
        val text = generateFileContent()
        File(filename).apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(text)
        }
    }

    fun compare(file: String) {
        val filename = generateFilename(file)
        val text = generateFileContent()
        val expectedText = File(filename).readText()
        assertEquals(expectedText, text)
    }

    private fun generateFileContent(): String {
        val header = "Usages of " + element.fqn
        return header + "\n" + usages
            .sortedBy { it.toString() }
            .joinToString("\n") {
                buildString {
                    append("   ")
                    append(it.location?.editor?.file)
                    append("\n")
                    append("      ")
                    append(it.toString())
                }
            }
    }

    private fun generateFilename(file: String): String {
        val elementName = element.fqn
            .trim('\\')
            .replace("\\", "_")
            .replace("::", "-")
        val inModuleFolder = if (inModule.isEmpty()) "" else "$inModule/"
        return ctx.ctx.ctx.realAbsolutePath("usages/$file") + "/$inModuleFolder/$elementName.txt"
    }
}

@TestDslMarker
class PhpElementContext(element: PsiElement, ctx: BaseFileContext) : ElementContextBase(element, ctx) {
    private val editor = FileEditorManager.getInstance(project).selectedTextEditor

    // Во время смены контекста, меняем каретку на текущий элемент
    init {
        val named = element as? PhpNamedElement
        if (named != null) {
            val offset = named.nameIdentifier?.startOffset
            if (offset != null) {
                editor?.caretModel?.moveToOffset(offset + 1)
            }
        }
    }

    fun makeInternal() {
        val modulite = element.containingModulite()
        val file = modulite?.configPsiFile()
        file?.makeElementInternal(element as PhpNamedElement)
        Thread.sleep(1000)
    }

    fun makeExported() {
        val modulite = element.containingModulite()
        val file = modulite?.configPsiFile()
        file?.makeElementExport(element as PhpNamedElement)
    }

    fun allowUsages(): Result {
        val modulite = element.containingModulite()
        val file = modulite?.configPsiFile()!!
        val res = Result(file.allowExternalUsages(element as PhpNamedElement))
        waitWithEventsDispatching(1) // wait for end of allow process
        return res
    }

    fun allowForModulite(name: String) {
        val modulite = element.containingModulite()
        val file = modulite?.configPsiFile()
        val forModulite = ModuliteIndex.getInstance(project).getModulite("@$name")
            ?: throw IllegalStateException("Can't find modulite named $name")
        file?.allowElementForModulite(element as PhpNamedElement, forModulite)
    }

    fun findUsagesInCurrentModule(block: FindUsagesContext.() -> Unit) {
        findUsagesImpl("", block) {
            PhpUsagesFinder().find(element)
        }
    }

    fun findUsagesInModule(name: String, block: FindUsagesContext.() -> Unit) {
        findUsagesImpl(name, block) {
            val modulite = ModuliteIndex.getInstance(project).getModulite("@$name")
                ?: throw IllegalStateException("Can't find modulite named $name")
            PhpUsagesFinder(modulite).find(element)
        }
    }

    fun assertIntentionExist(name: String) {
        fixture.findSingleIntention(name)
    }

    fun assertIntentionNotExist(name: String) {
        var isExist = false

        try {
            fixture.findSingleIntention(name)
            isExist = true
        } catch (_: AssertionError) {
        }

        if (isExist) {
            Assert.fail("Intention \"$name\" is exist")
        }
    }

    fun runIntention(name: String) {
        ctx.ctx.enableInspections(ChangeVisibilityEmptyInspection())
        ctx.ctx.enableInspections(AllowInternalAccessEmptyInspection())

        fixture.findSingleIntention(name).invoke(project, editor, ctx.file)
    }

    fun runQuickFixes() {
        ctx.ctx.enableInspections(ChangeVisibilityEmptyInspection())
        ctx.ctx.enableInspections(AllowInternalAccessEmptyInspection())
        ctx.ctx.enableInspections(InternalSymbolUsageInspection())

        fixture.getAllQuickFixes().forEach { qf ->
            fixture.launchAction(qf)
        }
    }
}

@TestDslMarker
class YamlElementContext(element: YAMLPsiElement, ctx: BaseFileContext) : ElementContextBase(element, ctx) {
    fun findUsagesInCurrentModule(block: FindUsagesContext.() -> Unit) {
        findUsagesImpl("", block) {
            YamlUsagesFinder().find(element)
        }
    }

    fun findUsagesInModule(name: String, block: FindUsagesContext.() -> Unit) {
        findUsagesImpl(name, block) {
            val modulite = ModuliteIndex.getInstance(project).getModulite("@$name")
                ?: throw IllegalStateException("Can't find modulite named $name")
            YamlUsagesFinder(modulite).find(element)
        }
    }
}

open class ElementContextBase(val element: PsiElement, val ctx: BaseFileContext) {
    protected val project = element.project
    protected val fixture = ctx.ctx.ctx.fixture

    fun findUsagesImpl(name: String, block: FindUsagesContext.() -> Unit, findAction: () -> Unit) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor!!

        val offset = element.startOffset
        editor.caretModel.moveToOffset(offset + 1)

        runInEdtAndWait {
            findAction()
        }

        val usages = runInEdtAndGet {
            val startMillis = System.currentTimeMillis()
            var view: UsageView?
            var viewWasInitialized = false
            while (UsageViewManager.getInstance(project).selectedUsageView.also { view = it } == null || view!!.isSearchInProgress) {
                IdeEventQueue.getInstance().flushQueue()
                viewWasInitialized = viewWasInitialized or (view != null)
                if (!viewWasInitialized && System.currentTimeMillis() - startMillis > TimeUnit.SECONDS.toMillis(10)) {
                    Assert.fail("UsageView wasn't shown")
                    return@runInEdtAndGet emptyList<Usage>()
                }
            }
            view!!.usages
        }

        if (element is YAMLQuotedText) {
            val el = element.resolveSymbol().firstOrNull() as? PhpNamedElement
            if (el == null) {
                fail("Can't resolve ${element.text}")
                return
            }
            FindUsagesContext(name, el, usages, this).block()
            return
        }

        if (element is PhpNamedElement) {
            FindUsagesContext(name, element, usages, this).block()
        }
    }
}

@TestDslMarker
class PhpContext(file: PhpFile, ctx: StepContext) : BaseFileContext(file, ctx) {
    fun element(name: String, block: PhpElementContext.() -> Unit) {
        val element = file.findElementByName(name) ?: throw IllegalStateException("Can't find element named $name")
        PhpElementContext(element, this).block()
    }

    fun reference(name: String, block: PhpElementContext.() -> Unit) {
        val element = file.findReferenceByName(name) ?: throw IllegalStateException("Can't find reference named $name")
        PhpElementContext(element, this).block()
    }

    fun eachDeclaration(block: PhpElementContext.() -> Unit) {
        file as PhpFile

        file.topLevelDefs.keySet().forEach {
            val elements = file.topLevelDefs[it]
            elements.forEach els@{ element ->
                if (element is PhpNamespace || element is Variable) {
                    return@els
                }

                PhpElementContext(element, this).block()
            }
        }
    }

    fun eachClassMembers(block: PhpElementContext.() -> Unit) {
        eachDeclaration {
            if (element is PhpClass) {
                element.methods.forEach {
                    PhpElementContext(it, this@PhpContext).block()
                }
                element.fields.forEach {
                    PhpElementContext(it, this@PhpContext).block()
                }
            }
        }
    }

    fun check(suffix: String) {
        val expectedVirtualFile = file.virtualFile.parent.findChild(file.name + suffix)
            ?: throw Exception("Expected file not found: ${file.name + suffix}")

        val expectedFile = expectedVirtualFile.psiFile<PsiFile>(file.project)!!
        assertEquals(expectedFile.text, file.text)
    }
}

@TestDslMarker
class YamlContext(file: YAMLFile, ctx: StepContext) : BaseFileContext(file, ctx) {
    fun element(name: String, block: YamlElementContext.() -> Unit) {
        val element = file.findQuotedTextByValue(name) ?: throw IllegalStateException("Can't find element name $name")
        YamlElementContext(element, this).block()
    }

    fun eachRequires(block: YamlElementContext.() -> Unit) {
        file as YAMLFile

        val requiresKeyValue = file.getTopLevelKey("require") ?: return
        val requiresSeq = requiresKeyValue.value as? YAMLSequence ?: return

        requiresSeq.items.forEach {
            val value = it.value as? YAMLQuotedText ?: return@forEach
            YamlElementContext(value, this).block()
        }
    }

    fun eachExports(block: YamlElementContext.() -> Unit) {
        file as YAMLFile

        val requiresKeyValue = file.getTopLevelKey("export") ?: return
        val requiresSeq = requiresKeyValue.value as? YAMLSequence ?: return

        requiresSeq.items.forEach {
            val value = it.value as? YAMLQuotedText ?: return@forEach
            YamlElementContext(value, this).block()
        }
    }

    fun assertExport(name: String) {
        file as YAMLFile
        file.exportList().find {
            it.name == name
        } ?: throw IllegalStateException("Can't find export $name")
    }

    fun check(suffix: String) {
        val expectedVirtualFile = file.virtualFile.parent.findChild(file.name + suffix)
            ?: throw Exception("Expected file not found: ${file.name + suffix}")

        val expectedFile = expectedVirtualFile.psiFile<PsiFile>(file.project)!!
        assertEquals(expectedFile.text, file.text)
    }

    fun generateRequires() {
        ModuliteDependenciesManager.regenerate(file.project, file.virtualFile)
        Thread.sleep(1000)
    }
}

open class BaseFileContext(val file: PsiFile, val ctx: StepContext)

@TestDslMarker
open class ModuliteContext(val modulite: Modulite, val ctx: StepContext) {
    fun allowInForModulite(inModuliteName: String, forModulite: String) {
        val element = modulite.namePsi()!!

        val inModulite = ModuliteIndex.getInstance(ctx.ctx.fixture.project).getModulite("@$inModuliteName")
            ?: throw IllegalStateException("Can't find modulite $inModuliteName")
        val file = inModulite.configPsiFile()
        file?.allowModulite("@$forModulite", element)
    }
}

class ModuliteContextData(
    var name: String,
    var namespace: Namespace,
    var description: String?,
    var folder: String,
    var exported: Boolean,
    var selectedSymbols: List<SymbolName>,
    var parent: Modulite?,
)

fun ModuliteContextData.to(): ModuliteBuilderData {
    return ModuliteBuilderData(
        name,
        namespace,
        description,
        folder,
        exported,
        selectedSymbols,
        parent
    )
}

@TestDslMarker
open class ModuliteCreationContext(val ctx: ModuliteContextData) {
    fun description(description: String) {
        ctx.description = description
    }
}

@TestDslMarker
class ModuliteFromSourcesContext(ctx: ModuliteContextData, private val collectedData: ModuliteData) : ModuliteCreationContext(ctx) {
    fun makeExported(name: String) {
        val symbolName = collectedData.symbols.firstOrNull {
            it.name == name
        } ?: throw IllegalArgumentException("Symbol $name not found")

        ctx.selectedSymbols += symbolName
    }

    fun makeAllExported() {
        ctx.selectedSymbols = collectedData.symbols
    }

    fun symbols(): List<SymbolName> {
        return collectedData.symbols
    }

    fun exportSymbols(symbols: List<SymbolName>) {
        ctx.selectedSymbols = symbols
    }

    fun exportFromParent() {
        ctx.exported = true
    }
}

@TestDslMarker
class DirContext(val folder: String, val ctx: StepContext) {
    fun dir(name: String, block: DirContext.() -> Unit) {
        DirContext("$folder/$name", ctx).block()
    }

    fun php(name: String, content: String, block: PhpContext.() -> Unit = {}) {
        val rel = ctx.filename("$folder/$name")
        val abs = ctx.realAbsolutePath("$folder/$name")

        File(abs).apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(content)
        }

        val file = ctx.ctx.fixture.configureByFile(rel)
        PhpContext(file as PhpFile, ctx).block()
    }
}

@TestDslMarker
class StepContext(val name: String, val ctx: IntegrationTestContext) {
    fun filename(filename: String): String {
        return File(ctx.subFolder, filename).path
    }

    fun realAbsolutePath(filename: String): String {
        return File(File(ctx.rootFolder, ctx.subFolder), filename).absolutePath
    }

    fun php(filename: String, block: PhpContext.() -> Unit) {
        assertTrue(filename.endsWith(".php"))
        val file = ctx.fixture.findFileInTempDir(filename(filename)) ?: throw Exception("File $filename not found")

        ctx.fixture.openFileInEditor(file)
        PhpContext(file.psiFile(ctx.fixture.project)!!, this).block()
    }

    fun yaml(filename: String, block: YamlContext.() -> Unit) {
        assertTrue(filename.endsWith(".yaml"))
        val file = ctx.fixture.findFileInTempDir(filename(filename)) ?: throw Exception("File $filename not found")

        ctx.fixture.openFileInEditor(file)
        YamlContext(file.psiFile(ctx.fixture.project)!!, this).block()
    }

    fun modulite(name: String, block: ModuliteContext.() -> Unit) {
        val modulite = modulite(name)
        ModuliteContext(modulite, this).block()
    }

    fun modulite(name: String): Modulite {
        return ModuliteIndex.getInstance(ctx.fixture.project).getModulite("@$name")
            ?: throw IllegalStateException("Modulite $name not found")
    }

    infix fun Modulite.canUse(rhs: Modulite): Result {
        return Result(canUse(rhs))
    }

    fun openedFile(): Result {
        val files = FileEditorManager.getInstance(ctx.fixture.project).selectedFiles
        val path = files.firstOrNull()?.path ?: return Result(null)
        val relative = path.removePrefix("/src/").removePrefix(ctx.subFolder + "/")
        return Result(relative)
    }

    fun createModulite(name: String, namespace: String, folderName: String, block: ModuliteCreationContext.() -> Unit = {}) {
        val data = ModuliteContextData(
            name = name,
            namespace = Namespace(namespace),
            description = null,
            folder = folderName,
            exported = false,
            selectedSymbols = emptyList(),
            parent = null,
        )
        val context = ModuliteCreationContext(data).apply(block)
        val folder = ctx.fixture.findFileInTempDir(filename(""))

        ModuliteBuilder(ctx.fixture.project).createRawModulite(folder) {
            context.ctx.to()
        }

        Utils.waitWithEventsDispatching(2)
    }

    fun dir(name: String, block: DirContext.() -> Unit) {
        DirContext(name, this).block()
    }

    fun createModuliteFromSource(folderName: String, block: ModuliteFromSourcesContext.() -> Unit = {}) {
        val folder = ctx.fixture.findFileInTempDir(filename(folderName))
        val parent = folder.containingModulite(ctx.fixture.project)

        ModuliteBuilder(ctx.fixture.project).createModuliteFromSource(folder, parent) { data ->
            val ctxData = ModuliteContextData(
                name = if (parent != null) parent.name + "/" + data.generatedName else data.generatedName,
                namespace = data.namespace,
                description = null,
                folder = folderName,
                exported = false,
                selectedSymbols = emptyList(),
                parent = parent,
            )
            val context = ModuliteFromSourcesContext(ctxData, data).apply(block)

            context.ctx.to()
        }

        Utils.waitWithEventsDispatching(1)
    }

    fun enableInspections(vararg inspections: LocalInspectionTool) {
        ctx.fixture.enableInspections(*inspections)
    }

    fun checkHighlights() {
        ctx.fixture.checkHighlighting(ctx.files)
    }
}

@TestDslMarker
class IntegrationTestContext {
    private val steps = mutableListOf<StepContext>()
    var files = listOf<String>()
    var rootFolder: String = ""
    var subFolder: String = ""
    lateinit var fixture: CodeInsightTestFixture

    fun fixture(fixture: CodeInsightTestFixture) {
        this.fixture = fixture
    }

    fun root(rootFolder: String, subFolder: String) {
        this.rootFolder = rootFolder
        this.subFolder = subFolder

        val testDataFolder = File(rootFolder, subFolder)
        BasePlatformTestCase.assertTrue(testDataFolder.exists())

        val walker = testDataFolder.walk()
        files = walker
            .filter { it.isFile && it.name != ".DS_Store" }
            .map { it.path.removePrefix(this.rootFolder) }
            .toList()

        fixture.configureByFiles(*files.toTypedArray())
    }

    fun step(name: String, step: StepContext.() -> Unit) {
        steps.add(StepContext(name, this).apply { step() })
    }
}

class Result(val data: Any?)

infix fun <T> Result.shouldBe(expected: T): Result {
    assertEquals(expected, data)
    return this
}

object integrationTestBase {
    operator fun invoke(init: IntegrationTestContext.() -> Unit) {
        IntegrationTestContext().init()
    }
}
