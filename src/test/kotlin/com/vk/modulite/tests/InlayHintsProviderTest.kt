package com.vk.modulite.tests

import com.vk.modulite.infrastructure.InlayHintsProviderTestBase

class InlayHintsProviderTest : InlayHintsProviderTestBase() {
    fun `test all export`()                          = runHintTest("Module/AllExport.php")
    fun `test hide method`()                         = runHintTest("Module/HideMethod.php")
    fun `test all internal`()                        = runHintTest("Module/AllInternal.php")
    fun `test auto export method`()                  = runHintTest("Module/AutoExportMethod.php")
    fun `test all allowed for module2`()             = runHintTest("Module/AllAllowedForModule2.php")
    fun `test all allowed for several modulites`()   = runHintTest("Module/AllAllowedForSeveralModulites.php")
    fun `test auto export method of allowed class`() = runHintTest("Module/AutoExportMethodOfAllowedClass.php")
    fun `test static and not static method`()        = runHintTest("Module/StaticNotStaticMethod.php")
    fun `test allow only method for module2`()       = runHintTest("Module/AllowMethodForModule.php")
}
