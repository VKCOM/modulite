package com.vk.modulite.tests

import com.vk.modulite.infrastructure.ModuliteInspectionTestBase
import com.vk.modulite.inspections.config.*

class YamlInspectionsTest : ModuliteInspectionTestBase() {
    fun `test invalid namespace`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(InvalidNamespaceInspection())
        runFixture("inspections/InvalidNamespace")
    }

    fun `test modulite redeclaration`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(ModuliteRedeclarationInspection())
        runFixture("inspections/ModuliteRedeclaration")
    }

    fun `test invalid modulite name`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(InvalidModuliteNameInspection())
        runFixture("inspections/InvalidModuliteName")
    }

    fun `test empty value in list`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(EmptyValueInListInspection())
        runFixture("inspections/EmptyValueInList")
    }

    fun `test unescaped back slash in name`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(UnescapedBackSlashInNameInspection())
        runFixture("inspections/UnescapedBackSlashInName")
    }

    fun `test unknown modulite`() {
        myFixture.enableInspections(UnknownModuliteInspection())
        runFixture("inspections/UnknownModulite")
    }

    fun `test unknown symbol`() {
        myFixture.enableInspections(UnknownSymbolInspection())
        runFixture("inspections/UnknownSymbol")
    }

    fun `test wrong modulite require`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(WrongRequireInspection())
        runFixture("inspections/WrongModuliteRequire")
    }

    fun `test unnecessary fully qualified name`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(UnnecessaryFullyQualifiedNameInspection())
        runFixture("inspections/UnnecessaryFullyQualifiedName")
    }

    fun `test require symbol from modulite`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(RequireSymbolFromModuliteInspection())
        runFixture("inspections/RequireSymbolFromModulite")
    }

    fun `test inconsistent nesting`() {
        myFixture.enableInspections(InconsistentNestingInspection())
        runFixture("inspections/InconsistentNesting")
    }

    fun `test wrong force internal`() {
        myFixture.enableInspections(WrongForceInternalInspection())
        runFixture("inspections/WrongForceInternal")
    }

    fun `test other yaml file with no inspections`() {
        myFixture.enableInspections(UnknownSymbolInspection())
        myFixture.enableInspections(UnknownModuliteInspection())
        myFixture.enableInspections(RequireSymbolFromModuliteInspection())
        myFixture.enableInspections(InconsistentNestingInspection())
        myFixture.enableInspections(InvalidNamespaceInspection())
        runFixture("inspections/NonModuliteYamls")
    }
}
