package com.vk.modulite.tests.integration

import com.vk.modulite.infrastructure.IntegrationTestBase

class MakeExportInternalTest : IntegrationTestBase() {
    fun `test make export and make internal`() = integrationTest {
        step("make element exported") {
            php("Module/functions.php") {
                eachDeclaration {
                    makeExported()
                }
            }

            yaml("Module/.modulite.yaml") {
                check(".expected.0")
            }
        }

        step("make element internal") {
            php("Module/functions.php") {
                eachDeclaration {
                    makeInternal()
                }
            }

            yaml("Module/.modulite.yaml") {
                check(".expected.1")
            }
        }
    }

    fun `test make internal class with explicitly internal members`() = integrationTest {
        step("change visibility") {
            php("Module/functions.php") {
                element("ModuleGlobalClass") {
                    makeExported()
                }

                element("staticMethod") {
                    makeInternal()
                }

                element("staticField") {
                    makeInternal()
                }

                element("CONSTANT") {
                    makeInternal()
                }
            }

            yaml("Module/.modulite.yaml") {
                check(".expected.2")
            }
        }

        step("make class internal") {
            php("Module/functions.php") {
                element("ModuleGlobalClass") {
                    makeInternal()
                }
            }

            yaml("Module/.modulite.yaml") {
                check(".expected.3")
            }
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/MakeExportInternal"
}
