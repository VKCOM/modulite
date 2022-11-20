package com.vk.modulite.tests.integration

import com.vk.modulite.dsl.shouldBe
import com.vk.modulite.infrastructure.IntegrationTestBase
import com.vk.modulite.inspections.InternalSymbolUsageInspection

class IntentionsTest : IntegrationTestBase() {
    fun `test go to modulite intention`() = integrationTest("GoToModulite") {
        step("invoke intention") {
            php("Module/functions.php") {
                element("module_func") {
                    runIntention("Go to modulite definition")
                }
            }
        }

        step("check") {
            openedFile() shouldBe "Module/.modulite.yaml"
        }
    }

    fun `test make export or internal intention`() = integrationTest("ChangeVisibility") {
        step("invoke intention") {
            php("Module/functions.php") {
                element("MODULE_GLOBAL_DEFINE") {
                    runIntention("Make exported")
                }

                element("MODULE_GLOBAL_CONST") {
                    runIntention("Make internal")
                }

                element("ModuleGlobalVariable") {
                    assertIntentionNotExist("Make exported")
                }

                element("CONSTANT") {
                    runIntention("Make internal")
                }

                element("staticMethod") {
                    runIntention("Make internal")
                }

                element("staticField") {
                    runIntention("Make internal")
                }

                element("staticField") {
                    runIntention("Make exported")
                }

                element("module_global_function") {
                    runIntention("Make exported")
                }
            }
        }

        step("check") {
            yaml("Module/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    fun `test allow internal access intention`() = integrationTest("AllowInternalAccess") {
        step("invoke intention") {
            php("Module/functions.php") {
                element("module_func") {
                    // This intention requires input from the user, so we check it directly.
                    // runIntention("Allow internal symbol")
                    allowForModulite("module2")
                }
            }
        }

        step("check") {
            yaml("Module/.modulite.yaml") {
                check(".expected")
            }
        }

        step("check highlights") {
            enableInspections(InternalSymbolUsageInspection())
            checkHighlights()
        }
    }

    fun `test check intention for not static or not public`() = integrationTest("NotStaticNotPublic") {
        step("invoke intention") {
            for (type in listOf("fields", "methods")) {
                php("Module/$type.php") {
                    element("Foo") {
                        runIntention("Make exported")
                    }

                    element("publicStatic") {
                        assertIntentionExist("Make internal")
                        assertIntentionNotExist("Make exported")
                    }

                    element("privateStatic") {
                        assertIntentionNotExist("Make internal")
                        assertIntentionNotExist("Make exported")
                    }

                    element("publicNotStatic") {
                        assertIntentionNotExist("Make internal")
                        assertIntentionNotExist("Make exported")
                    }

                    element("privateNotStatic") {
                        assertIntentionNotExist("Make internal")
                        assertIntentionNotExist("Make exported")
                    }

                    element("Foo") {
                        runIntention("Make internal")
                    }
                }
            }
        }
    }


    override fun getTestDataPath() = "src/test/fixtures/integration/Intentions"
}
