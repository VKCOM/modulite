package com.vk.modulite.psi.extensions.php

import com.jetbrains.php.lang.PhpLangUtil
import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.impl.*
import com.vk.modulite.SymbolName
import com.vk.modulite.modulite.Modulite

fun PhpNamedElement.symbolName(
    reference: PhpReference? = null,
    forNotRequired: Boolean = false,
    forNotPublic: Boolean = false,
): SymbolName {
    val normalized = fqn
        .replace(".", "::")

    if (this is VariableImpl) {
        val parts = normalized.removePrefix("\\").split("\\")
        val lastPart = parts.last()
        return SymbolName("$$lastPart", kind = SymbolName.Kind.GlobalVariable)
    }

    if (this is MethodImpl) {
        if ((forNotRequired || forNotPublic) && PhpLangUtil.isMagicMethod(this.name)) {
            val containingClass = this.containingClass
            if (containingClass != null) {
                return containingClass.symbolName(reference)
            }
        }

        val className = resolveUsedClass(reference) ?: return SymbolName("$normalized()", kind = SymbolName.Kind.Method)
        val methodName = name
        val fqn = "$className::$methodName"
        return SymbolName("$fqn()", kind = SymbolName.Kind.Method)
    }

    if (this is FieldImpl && !this.isConstant) {
        if (forNotRequired) {
            val containingClass = this.containingClass
            if (containingClass != null) {
                return containingClass.symbolName(reference)
            }
        }

        val className = resolveUsedClass(reference) ?: return SymbolName(normalized, kind = SymbolName.Kind.Field)
        val fieldName = name
        val fqn = "$className::$$fieldName"
        return SymbolName(fqn, kind = SymbolName.Kind.Field)
    }

    if (this is ClassConstImpl) {
        if (forNotRequired) {
            val containingClass = this.containingClass
            if (containingClass != null) {
                return containingClass.symbolName(reference)
            }
        }

        val className = resolveUsedClass(reference) ?: return SymbolName(normalized, kind = SymbolName.Kind.ClassConstant)
        val constName = name
        val fqn = "$className::$constName"
        return SymbolName(fqn, kind = SymbolName.Kind.ClassConstant)
    }

    if (this is FunctionImpl) {
        return SymbolName("$normalized()", kind = SymbolName.Kind.Function)
    }

    if (this is ConstantImpl) {
        return SymbolName(normalized, kind = SymbolName.Kind.Constant)
    }

    val kind = when (this) {
        is PhpClass      -> SymbolName.Kind.Class
        is FieldImpl     -> SymbolName.Kind.Field
        is PhpDefineImpl -> SymbolName.Kind.Constant
        else             -> SymbolName.Kind.Unknown
    }

    return SymbolName(normalized, kind = kind)
}

private fun resolveUsedClass(reference: PhpReference?): String? {
    if (reference is MemberReference && reference.classReference is ClassReference) {
        val klass = (reference.classReference as ClassReference).resolve()
        if (klass != null && klass is PhpClass) {
            return klass.fqn
        }
    }

    return null
}

fun PhpNamedElement.modulitesWithAllowedAccess(module: Modulite): List<SymbolName> {
    val name = symbolName()
    val className = name.className()

    return module.allowedInternalAccess.mapNotNull { (moduleName, symbols) ->
        if (moduleName.kind != SymbolName.Kind.Modulite) {
            return@mapNotNull null
        }

        val isAllowed = symbols.any { symbol ->
            if (name.isClassMember()) {
                return@any className.equals(symbol, module) || name.equals(symbol, module)
            }

            name.equals(symbol, module)
        }

        if (isAllowed) {
            moduleName
        } else {
            null
        }
    }
}

fun PhpNamedElement.placesWithAllowedAccess(module: Modulite): List<SymbolName> {
    val name = symbolName()

    return module.allowedInternalAccess.mapNotNull { (place, symbols) ->
        if (place.kind == SymbolName.Kind.Modulite) {
            return@mapNotNull null
        }

        val isAllowed = symbols.any { symbol ->
            name.equals(symbol, module)
        }

        if (isAllowed) {
            place
        } else {
            null
        }
    }
}

/**
 * По каким-то причинам для констант fqn бросает исключение, поэтому этот
 * метод нужен, чтобы получить имя не опасаясь его.
 */
fun PhpReference.safeFqn(): String {
    return try {
        fqn ?: ""
    } catch (e: UnsupportedOperationException) {
        signature
    }
}
