package com.vk.modulite.tests.integration

import com.vk.modulite.infrastructure.IntegrationTestBase

class PhpFindUsagesTest : IntegrationTestBase() {
    fun `test find usages in current module`() = integrationTest("InCurrentModule") {
        step("find usages") {
            php("Module/functions.php") {
                eachDeclaration {
                    findUsagesInCurrentModule {
                        compare("Module/current_module")
                    }
                }
            }
        }
    }

    fun `test find usages in other module`() = integrationTest("InOtherModule") {
        step("find usages") {
            php("Module/functions.php") {
                eachDeclaration {
                    findUsagesInModule("module2") {
                        compare("Module/other_module")
                    }
                    findUsagesInModule("module2/sub-module") {
                        compare("Module/other_module")
                    }
                }
            }
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/FindUsages/php"
}
