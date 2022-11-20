package com.vk.modulite.modulite

import com.intellij.psi.util.findParentOfType
import com.intellij.psi.util.findTopmostParentInFile
import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.impl.FunctionImpl
import com.vk.modulite.SymbolName
import com.vk.modulite.composer.ComposerPackage
import com.vk.modulite.psi.extensions.files.containingComposerPackage
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.psi.extensions.php.symbolName
import com.vk.modulite.utils.fromStubs
import com.vk.modulite.utils.fromVendor

object ModuliteRestrictionChecker {
    enum class ViolationTypes {
        Ok,
        NotPublic,
        NotRequired,
        CantUseNestedModulite,
    }

    data class Context(
        val element: PhpPsiElement,
        val modulite: Modulite?,
        val composerPackage: ComposerPackage?,
        val function: PhpNamedElement?,
        val klass: PhpNamedElement?,
    )

    fun Context.kind() = when {
        modulite != null                  -> SymbolName.Kind.Modulite
        composerPackage != null           -> SymbolName.Kind.ComposerPackage
        function != null && klass != null -> SymbolName.Kind.Method
        function != null                  -> SymbolName.Kind.Function
        klass != null                     -> SymbolName.Kind.Class
        else                              -> SymbolName.Kind.Unknown
    }

    fun Context.name() = when {
        modulite != null        -> modulite.symbolName()
        composerPackage != null -> composerPackage.symbolName()
        function != null        -> function.symbolName()
        klass != null           -> klass.symbolName()
        else                    -> SymbolName("unknown", SymbolName.Kind.Unknown)
    }

    fun Context.places(): List<SymbolName> {
        val result = mutableListOf<SymbolName>()
        if (modulite != null) {
            result.add(modulite.symbolName())
        }

        if (composerPackage != null) {
            result.add(composerPackage.symbolName())
        }

        if (function != null) {
            result.add(function.symbolName())
        }

        if (klass != null) {
            result.add(klass.symbolName())
        }

        return result
    }

    fun createContext(element: PhpPsiElement): Context {
        val file = element.containingFile.virtualFile
        val modulite = file.containingModulite(element.project)
        val composerPackage = file.containingComposerPackage(element.project)
        // Мы не можем просто искать первую FunctionImpl, так как ей может
        // оказаться лямбда, которую нужно пропустить.
        val function = element.findTopmostParentInFile { it is FunctionImpl && it.name.isNotEmpty() } as? FunctionImpl
        val klass = element.findParentOfType<PhpClass>(true)
        return Context(element, modulite, composerPackage, function, klass)
    }

    /**
     * Проверяет можно ли использовать [element] в текущем [context].
     *
     * Если элементом является метод или свойство, то нам важно как он был вызван ([reference]).
     * Так, например, если класс А наследуется от внутреннего класса Б, то внутренний метод класса Б
     * может быть использован через класс А.
     *
     * ```
     * A::internalFromParent() // ok
     * B::internalFromParent() // error
     * ```
     *
     * Это необходимо чтобы при наследовании можно было использовать внутренние методы или свойства
     * родительского класса, иначе метод пришлось бы делать явно экспортированным, что не так хорошо.
     */
    fun canUse(context: Context, element: PhpNamedElement, reference: PhpReference): Pair<Boolean, ViolationTypes> {
        val file = element.containingFile?.virtualFile ?: return result(ViolationTypes.Ok)

        if (context.element is ClassReference) {
            if (context.element.text == "static" || context.element.text == "self") {
                return result(ViolationTypes.Ok)
            }
        }

        if (element is Method && PhpLangUtil.isMagicMethod(element.name)) {
            val containingClass = element.containingClass
            if (containingClass != null) {
                return canUse(context, containingClass, reference)
            }
        }

        // Не проверяем использования символов, которые определены в
        // стабах или являются сторонними.
        if (file.fromStubs() || file.fromVendor()) {
            return result(ViolationTypes.Ok)
        }

        val name = element.symbolName(reference)
        val modulite = element.containingModulite(reference)

        // В случае, когда контекст в котором используется символ
        // не является модулем, то напрямую проверяем это использование.
        if (context.modulite == null) {
            return canUseSymbol(context, element, reference, modulite)
        }

        // Если символ используется в пакете в котором он определен.
        val composerPackage = element.containingComposerPackage()
        if (context.composerPackage != null && composerPackage != null) {
            if (context.composerPackage.name == composerPackage.name) {
                return canUseSymbol(context, element, reference, modulite)
            }
        }

        // Сначала мы должны проверить не находится ли символ в composer пакете,
        // если так, то проверяем что он есть в зависимостях.
        if (composerPackage != null) {
            val required = context.modulite.requires.composerPackages().contains(composerPackage.symbolName())
            if (required) {
                return canUseSymbol(context, element, reference, modulite)
            }

            return result(ViolationTypes.NotRequired)
        }

        // Если символ определен в глобальном коде, то
        // ищем его имя в requires.
        if (modulite == null) {
            val required = context.modulite.containsInRequires(name)
            if (required) {
                return result(ViolationTypes.Ok)
            }

            if (context.element is ClassReference) {
                // Для статических методов, полей и классовых констант не нужно искать текущий класс в requires
                when (context.element.context) {
                    is ClassConstantReference, is MethodReference, is FieldReference -> {
                        return result(ViolationTypes.Ok)
                    }
                }
            }

            if (context.element is MemberReference) {
                if (context.element.classReference?.text == "static" || context.element.classReference?.text == "self") {
                    return result(ViolationTypes.Ok)
                }
            }

            return result(ViolationTypes.NotRequired)
        }

        // Если символ используется в модуле в котором он определен.
        if (context.modulite.name == modulite.name) {
            return result(ViolationTypes.Ok)
        }

        val required = context.modulite.requires.modulites().contains(modulite.symbolName())
        if (required) {
            return canUseSymbol(context, element, reference, modulite)
        }

        return result(ViolationTypes.NotRequired)
    }

    private fun canUseSymbol(
        context: Context,
        element: PhpNamedElement,
        reference: PhpReference,
        elementModulite: Modulite?,
    ): Pair<Boolean, ViolationTypes> {
        if (elementModulite == null) {
            return result(ViolationTypes.Ok)
        }

        // Если символ используется в модуле в котором он определен.
        if (context.modulite?.name == elementModulite.name) {
            return result(ViolationTypes.Ok)
        }

        if (elementModulite.isExport(element, reference)) {
            // Если мы используем в контексте другого модуля.
            if (context.modulite != null) {
                // Проверяем, что модуль в котором определен символ
                // может быть использован из текущего модуля.
                if (!context.modulite.canUse(elementModulite)) {
                    return result(ViolationTypes.CantUseNestedModulite)
                }

                return result(ViolationTypes.Ok)
            }

            // В случае глобального кода, проверяем что модуль в котором определен символ
            // может быть использован из глобального кода.
            if (!elementModulite.canUseGlobally(context)) {
                return result(ViolationTypes.CantUseNestedModulite)
            }

            return result(ViolationTypes.Ok)
        }

        if (elementModulite.symbolInAllowedInternalAccess(element, context, reference)) {
            return result(ViolationTypes.Ok)
        }

        return result(ViolationTypes.NotPublic)
    }

    private fun result(type: ViolationTypes): Pair<Boolean, ViolationTypes> {
        if (type != ViolationTypes.Ok) {
            return false to type
        }
        return true to type
    }
}
