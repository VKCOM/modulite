package com.vk.modulite.tests

import com.vk.modulite.infrastructure.ModuliteInspectionTestBase

class GoldenTests : ModuliteInspectionTestBase() {
    fun `test modulite availability`()                     = runFixture("golden/ModuliteAvailability")
    fun `test simple good project`()                       = runFixture("golden/SimpleProject")
    fun `test allow internal rules`()                      = runFixture("golden/AllowInternal") // from KPHP tests
    fun `test instance methods`()                          = runFixture("golden/InstanceMethods") // from KPHP tests
    fun `test require nested modulites`()                  = runFixture("golden/RequireNestedModulites") // from KPHP tests
    fun `test check constants`()                           = runFixture("golden/CheckConstants") // from KPHP tests
    fun `test check globals`()                             = runFixture("golden/CheckGlobals") // from KPHP tests
    fun `test check functions and methods calls`()         = runFixture("golden/CheckCalls") // from KPHP tests
    fun `test extends`()                                   = runFixture("golden/Extends")
    fun `test modulite redeclaration`()                    = runFixture("golden/ModuliteRedeclaration")
    fun `test allow internal access in global`()           = runFixture("golden/AllowInternalAccessInGlobal")
    fun `test allow internal nested modulite for global`() = runFixture("golden/AllowInternalModuliteForGlobal")
    fun `test inherit classes and traits`()                = runFixture("golden/InheritAndTraits")
    fun `test inherit static field and class const`()      = runFixture("golden/InheritFieldAndConst")

    // packages
    fun `test simple packages`()        = runFixture("golden/Packages/SimplePackages")
    fun `test package with modulites`() = runFixture("golden/Packages/PackageWithModulites")
    fun `test symbols in module`()      = runFixture("golden/Packages/SymbolsInModule")
}
