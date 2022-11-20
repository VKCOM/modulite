package com.vk.modulite.tests.integration

import com.vk.modulite.dsl.StepContext
import com.vk.modulite.dsl.shouldBe
import com.vk.modulite.infrastructure.IntegrationTestBase
import com.vk.modulite.inspections.InternalSymbolUsageInspection
import org.junit.Ignore

class AllowUsagesTest : IntegrationTestBase() {
    @Ignore
    data class Case(
        val fileName: String,
        val elementName: String,
        val expectedCountUsages: Int,
    )

    private val cases = listOf(
        Case(
            "TestClass.php",
            "TestClass",
            expectedCountUsages = 7,
        ),
        Case(
            "test_function.php",
            "test_function",
            expectedCountUsages = 7,
        ),
        Case(
            "test_constant.php",
            "TEST_CONSTANT",
            expectedCountUsages = 7,
        ),
        Case(
            "test_define.php",
            "TEST_DEFINE",
            expectedCountUsages = 7,
        ),
        Case(
            "test_method.php",
            "test_method",
            expectedCountUsages = 7,
        ),
        Case(
            "test_method.php",
            "test_field",
            expectedCountUsages = 7,
        ),
        Case(
            "test_method.php",
            "TEST_CONSTANT",
            expectedCountUsages = 7,
        ),
    )

    fun `test allow usages`() = integrationTest {
        step("test allow usages") {
            cases.forEachIndexed { index, case ->
                allowUsagesStep(
                    index = index,
                    folder = "Module1",
                    fileName = case.fileName,
                    elementName = case.elementName,
                    expectedCountUsages = case.expectedCountUsages
                )
            }
        }

        step("check highlights") {
            enableInspections(InternalSymbolUsageInspection())
            checkHighlights()
        }
    }

    private fun StepContext.allowUsagesStep(
        index: Int,
        folder: String,
        fileName: String,
        elementName: String,
        expectedCountUsages: Int,
    ) {
        php("$folder/$fileName") {
            element(elementName) {
                allowUsages() shouldBe expectedCountUsages
                makeInternal()
            }
        }

        yaml("$folder/.modulite.yaml") {
            check(".expected.$index")
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/AutoAllowWhenMakeInternal"
}
