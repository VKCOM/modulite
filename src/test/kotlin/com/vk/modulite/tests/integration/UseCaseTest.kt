package com.vk.modulite.tests.integration

import com.vk.modulite.infrastructure.IntegrationTestBase

class UseCaseTest : IntegrationTestBase() {
    fun `test use cases`() = integrationTest {
        step("create modulite") {
            createModulite("messages", "\\Messages\\", "Messages")

            dir("Messages") {
                php("index.php", """<?php
                    namespace Messages;
                    
                    function foo() {}
                """)
            }

            php("Messages/index.php") {
                element("foo") {
                    makeExported()
                }
            }

            yaml("Messages/.modulite.yaml") {
                assertExport("foo()")
            }
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/UseCase"
}
