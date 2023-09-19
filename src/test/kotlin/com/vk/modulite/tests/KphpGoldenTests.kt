package com.vk.modulite.tests

import com.vk.modulite.infrastructure.ModuliteInspectionTestBase

class KphpGoldenTests : ModuliteInspectionTestBase() {
    fun `test 001_simple_yaml_project`()    = runFixture("kphp_golden/001_simple_yaml_project")
    fun `test 002_ok_require_submod`()      = runFixture("kphp_golden/002_ok_require_submod")
    fun `test 003_allow_internal`()         = runFixture("kphp_golden/003_allow_internal")
    fun `test 004_instance_methods`()       = runFixture("kphp_golden/004_instance_methods")
    fun `test 005_inheritance`()            = runFixture("kphp_golden/005_inheritance")
    fun `test 006_known_bugs`()             = runFixture("kphp_golden/006_known_bugs")
    fun `test 008_mod_generics`()           = runFixture("kphp_golden/008_mod_generics")
    fun `test 009_mod_magic_m`()            = runFixture("kphp_golden/009_mod_magic_m")
    fun `test 010_mod_unreachable`()        = runFixture("kphp_golden/010_mod_unreachable")
    fun `test 011_late_static_bindings`()   = runFixture("kphp_golden/011_late_static_bindings")
    fun `test 012_package_not_required_in_derived_class`()
        = runFixture("kphp_golden/012_package_not_required_in_derived_class")
    fun `test 013_inheritance_calling_class_inside_parent`()
        = runFixture("kphp_golden/013_inheritance_calling_class_inside_parent")
    // На потом
    //fun `test 007_composer_ok`()          = runFixture("kphp_golden/007_composer_ok")

    fun `test 100_wrong_static_bindings`()  = runFixture("kphp_golden/100_wrong_static_bindings")
}
