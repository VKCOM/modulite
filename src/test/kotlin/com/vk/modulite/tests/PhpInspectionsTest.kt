package com.vk.modulite.tests

import com.vk.modulite.infrastructure.ModuliteInspectionTestBase
import com.vk.modulite.inspections.ComposerPackageAnnotationUsageInspection
import com.vk.modulite.inspections.WrongNamespaceInspection

class PhpInspectionsTest : ModuliteInspectionTestBase() {
    fun `test composer package annotation usage`() {
        myFixture.enableInspections(ComposerPackageAnnotationUsageInspection())
        runFixture("inspections/ComposerPackageAnnotationUsage")
    }

    fun `test wrong namespace`() {
        myFixture.enableInspections(WrongNamespaceInspection())
        runFixture("inspections/WrongNamespace")
    }
}
