package com.vk.modulite.tests.integration

import com.vk.modulite.infrastructure.IntegrationTestBase
import com.vk.modulite.inspections.InternalSymbolUsageInspection

class CreateModuliteTest : IntegrationTestBase() {
    fun `test creation of simple modulite`() = integrationTest("Simple") {
        step("create modulite") {
            createModulite("@simple-module", "\\SimpleModule\\", "SimpleModule") {
                description("This is a simple modulite")
            }
        }

        step("check") {
            yaml("SimpleModule/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    fun `test creation of simple modulite from source`() = integrationTest("SimpleFromSource") {
        step("create modulite") {
            createModuliteFromSource("Module") {
                makeAllExported()
            }
        }

        step("check") {
            yaml("Module/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    fun `test creation of modulite from source with nested modulites`() = integrationTest("OneDepthNestedModulites") {
        step("create modulite") {
            createModuliteFromSource("Messages") {
                makeAllExported()
            }
        }

        step("check new modulite config") {
            yaml("Messages/.modulite.yaml") {
                check(".expected")
            }
        }

        step("check nested modulite config") {
            yaml("Messages/Core/.modulite.yaml") {
                check(".expected")
            }
            yaml("Messages/Folders/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    fun `test creation of modulite from source with deep nested modulites`() = integrationTest("DeepDepthNestedModulites") {
        step("create modulite") {
            createModuliteFromSource("Messages") {
                makeAllExported()
            }
        }

        step("check new modulite config") {
            yaml("Messages/.modulite.yaml") {
                check(".expected")
            }
        }

        step("check nested modulite config") {
            yaml("Messages/Core/.modulite.yaml") {
                check(".expected")
            }
            yaml("Messages/Core/Utils/.modulite.yaml") {
                check(".expected")
            }
            yaml("Messages/Core/Utils/SubUtils/.modulite.yaml") {
                check(".expected")
            }
            yaml("Messages/Folders/.modulite.yaml") {
                check(".expected")
            }
        }
    }

    fun `test regenerate other modulites`() = integrationTest("RegenerateOtherModulites") {
        step("create modulite") {
            createModuliteFromSource("Messages") {
                makeAllExported()
            }
        }

        step("check new modulite config") {
            yaml("Messages/.modulite.yaml") {
                check(".expected")
            }
        }

        step("check @users modulite config") {
            yaml("Users/.modulite.yaml") {
                check(".expected")
            }
        }

        step("check highlights") {
            enableInspections(InternalSymbolUsageInspection())
            checkHighlights()
        }
    }

    fun `test nested when parent export symbol`() = integrationTest("NestedWhenParentExportSymbol") {
        step("create modulite") {
            createModuliteFromSource("Module/FeatureModule") {
                makeAllExported()
            }
        }

        step("check modulite config") {
            yaml("Module/.modulite.yaml") {
                check(".expected")
            }
        }

        step("check feature modulite config") {
            yaml("Module/FeatureModule/.modulite.yaml") {
                check(".expected")
            }
        }

        step("check highlights") {
            enableInspections(InternalSymbolUsageInspection())
            checkHighlights()
        }
    }

    override fun getTestDataPath() = "src/test/fixtures/integration/CreateModulite"
}
