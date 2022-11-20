package com.vk.modulite.infrastructure

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.applyIf
import com.vk.modulite.inspections.InternalSymbolUsageInspection
import com.vk.modulite.inspections.config.ModuliteRedeclarationInspection
import com.vk.modulite.inspections.config.WrongRequireInspection
import java.io.File

abstract class ModuliteInspectionTestBase : BasePlatformTestCase() {
    companion object {
        private const val TEST_DATA_PATH = "src/test/fixtures/"
        private const val GEN_TEST_DATA_PATH = "src/test/fixtures/gen/"
        private const val PHP_COMMENT_START = "//"
        private const val YAML_COMMENT_START = "#"
        private const val TAB_IN_SPACES = "  "
        private const val PREFIX = "[modulite] "
    }

    override fun getTestDataPath() = "src/test/fixtures/gen"

    override fun setUp() {
        super.setUp()

        myFixture.enableInspections(
            WrongRequireInspection(),
            InternalSymbolUsageInspection(),
            ModuliteRedeclarationInspection()
        )
    }

    /**
     * Turn:
     * $_ = new Modulite();
     * //  ^^^^^^^^
     * //  Some error message
     *
     * To:
     * $_ = new <error descr="Some error message">Modulite</error>();
     */
    private fun generateTestFixtures(dir: String) {
        val testDataFolder = File(TEST_DATA_PATH + dir)
        val genTestDataFolder = File(GEN_TEST_DATA_PATH + dir)
        try {
            genTestDataFolder.deleteRecursively()
            testDataFolder.copyRecursively(genTestDataFolder, overwrite = true)
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val walker = genTestDataFolder.walk()
        walker.forEach {
            if (it.isDirectory) {
                return@forEach
            }
            if (it.extension != "php" && it.extension != "yaml") {
                return@forEach
            }

            val lines = it.readText().lines().toMutableList()

            var i = 1
            while (i < lines.size) {
                val cursor = lines[i].replace("\t", TAB_IN_SPACES)
                val trimmed = cursor.trim()

                if ((!trimmed.startsWith(PHP_COMMENT_START) && !trimmed.startsWith(YAML_COMMENT_START)) ||
                    !trimmed.contains("^")
                ) {
                    i++
                    continue
                }

                val lineWithCodeIndex = i - 1
                val lineWithCode = lines[lineWithCodeIndex].replace("\t", TAB_IN_SPACES)
                val lineWithError = lines[i + 1]
                val errorMessageRaw = lineWithError
                    .applyIf(lineWithError.indexOf('#') < 10) {
                        substringAfter(YAML_COMMENT_START)
                    }
                    .substringAfter(PHP_COMMENT_START)
                    .trim()

                if (!errorMessageRaw.contains(':')) {
                    return@forEach
                }

                val severity = errorMessageRaw.substringBefore(':').trim()
                var errorMessage = errorMessageRaw.substringAfter(':').trim()

                while (true) {
                    val line = lines[i + 2].trim()
                    if (!line.startsWith(PHP_COMMENT_START) && !line.startsWith(YAML_COMMENT_START)) {
                        break
                    }

                    errorMessage += "\n" + line
                        .applyIf(lineWithError.indexOf('#') < 10) {
                            substringAfter(YAML_COMMENT_START)
                        }
                        .substringAfter(PHP_COMMENT_START)
                        .trim()

                    i++
                }

                val startIndex = cursor.indexOf('^')
                val lastIndex = cursor.lastIndexOf('^')

                val newLine = lineWithCode.substring(0, startIndex) +
                        "<$severity descr=\"$PREFIX$errorMessage\">" +
                        lineWithCode.substring(startIndex, lastIndex + 1) +
                        "</$severity>" +
                        lineWithCode.substring(lastIndex + 1)

                lines[lineWithCodeIndex] = newLine

                i++
            }

            it.writeText(lines.joinToString("\n"))
        }
    }

    /**
     * Run inspection on file.fixture.php and check that all <warning> and <error> match
     * If file.qf.php exists, apply quickfixes and compare result to file.qf.php
     */
    protected fun runFixture(dir: String) {
        generateTestFixtures(dir)

        LocalFileSystem.getInstance().refresh(false)

        val genTestDataFolder = File(GEN_TEST_DATA_PATH + dir)
        assertTrue(genTestDataFolder.exists())

        val walker = genTestDataFolder.walk()
        val files = walker
            .filter { it.isFile && (it.extension == "php" || it.extension == "yaml" || it.extension == "qf" || it.extension == "json") }
            .map { it.path.removePrefix(GEN_TEST_DATA_PATH) }
            .toList().toTypedArray()

        myFixture.configureByFiles(*files)

        files.forEach {
            val file = myFixture.findFileInTempDir(it) ?: return@forEach
            myFixture.openFileInEditor(file)
            myFixture.checkHighlighting(true, false, true)

            val qfName = File(it).path + ".qf"

            val qfFile = myFixture.findFileInTempDir(qfName)
            if (qfFile != null) {
                myFixture.getAllQuickFixes().forEach { qf -> myFixture.launchAction(qf) }
                myFixture.checkResultByFile(qfName)
            }
        }
    }
}
