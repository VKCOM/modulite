package com.vk.modulite.modulite

import com.vk.modulite.SymbolName

data class ModuliteRequires(val symbols: List<SymbolName>) {
    fun modulites() = symbols.filter { it.kind == SymbolName.Kind.Modulite }
    fun composerPackages() = symbols.filter { it.kind == SymbolName.Kind.ComposerPackage }
    fun classes() = symbols.filter { it.kind == SymbolName.Kind.Class }
    fun methods() = symbols.filter { it.kind == SymbolName.Kind.Method }
    fun fields() = symbols.filter { it.kind == SymbolName.Kind.Field }
    fun functions() = symbols.filter { it.kind == SymbolName.Kind.Function }
    fun globalVariables() = symbols.filter { it.kind == SymbolName.Kind.GlobalVariable }
    fun classConstants() = symbols.filter { it.kind == SymbolName.Kind.ClassConstant }
    fun constants() = symbols.filter { it.kind == SymbolName.Kind.Constant }
}
