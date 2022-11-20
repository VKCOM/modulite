package com.vk.modulite.tests.integration

import com.vk.modulite.infrastructure.IntegrationTestBase

class RequiresGenerationTest : IntegrationTestBase() {
    fun `test generate requires from globals`() = integrationTest("GlobalSymbols") {
        step("generate requires") {
            yaml("Module/.modulite.yaml") {
                generateRequires()
            }

            yaml("Module/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    fun `test generate requires from other modulites`() = integrationTest("ModuliteSymbols") {
        step("generate requires") {
            yaml("Module/.modulite.yaml") {
                generateRequires()
            }

            yaml("Module/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    fun `test generate require of nested modulite`() = integrationTest("ModuliteSymbols") {
        step("generate requires") {
            yaml("Module3/.modulite.yaml") {
                generateRequires()
            }

            yaml("Module3/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/RequiresGeneration"
}