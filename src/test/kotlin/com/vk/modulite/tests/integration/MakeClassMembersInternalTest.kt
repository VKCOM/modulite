package com.vk.modulite.tests.integration

import com.vk.modulite.infrastructure.IntegrationTestBase

class MakeClassMembersInternalTest : IntegrationTestBase() {
    fun `test make class members export and make internal`() = integrationTest {
        step("make class members internal") {
            php("Module/functions.php") {
                eachClassMembers {
                    makeInternal()
                }
            }

            yaml("Module/.modulite.yaml") {
                check(".expected.0")
            }
        }

        step("make class members exported") {
            php("Module/functions.php") {
                eachClassMembers {
                    makeExported()
                }
            }

            yaml("Module/.modulite.yaml") {
                check(".expected.1")
            }
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/MakeClassMembersInternal"
}
