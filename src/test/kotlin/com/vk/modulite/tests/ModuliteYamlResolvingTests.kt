package com.vk.modulite.tests

import com.vk.modulite.infrastructure.ResolvingTestBase

class ModuliteYamlResolvingTests : ResolvingTestBase() {
    fun `test references resolving`() = runFixture("resolving")
}
