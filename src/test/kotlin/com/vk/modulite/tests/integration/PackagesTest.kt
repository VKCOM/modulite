package com.vk.modulite.tests.integration

import com.vk.modulite.infrastructure.IntegrationTestBase

class PackagesTest : IntegrationTestBase() {
    fun `test add package to requires via regeneration`() = integrationTest("AddPackageToRequires") {
        step("add package to requires via regeneration") {
            yaml("monolith/User/.modulite.yaml") {
                generateRequires()

                check(".expected")
            }
        }
    }

    fun `test add package to requires via quick fix`() = integrationTest("AddPackageToRequires") {
        step("add package to requires via regeneration") {
            php("monolith/User/main.php") {
                reference("Executor") {
                    runQuickFixes()
                }
            }

            yaml("monolith/User/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/Packages"
}
