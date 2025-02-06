package com.vk.modulite.infrastructure

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.vk.modulite.dsl.IntegrationTestContext
import com.vk.modulite.dsl.integrationTestBase

abstract class IntegrationTestBase : BasePlatformTestCase() {
    protected fun integrationTest(folder: String = "", init: IntegrationTestContext.() -> Unit) {
        integrationTestBase {
            fixture(myFixture)
            root(testDataPath, folder)
            init()
        }
    }
}
