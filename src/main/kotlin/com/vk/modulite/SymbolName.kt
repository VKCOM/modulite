package com.vk.modulite

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNamedElement
import com.intellij.util.applyIf
import com.jetbrains.php.PhpIndex
import com.vk.modulite.modulite.Modulite
import com.vk.modulite.modulite.ModuliteBase
import com.vk.modulite.services.ComposerPackagesIndex
import com.vk.modulite.services.ModuliteIndex

/**
 * Represent name of some symbol (PHP class, function, modulite or composer package).
 */
class SymbolName(rawName: String, val kind: Kind = supposeKind(normalize(rawName)), fromYaml: Boolean = false) : Comparable<SymbolName> {
    val name = if (fromYaml) normalize(rawName) else rawName

    enum class Kind {
        Modulite,
        ComposerPackageModulite, // #vk/common/@name
        ComposerPackage,
        Class,
        Method,
        Field,
        ClassConstant,
        Function,
        Constant,
        GlobalVariable,

        Unknown,
    }

    fun relative(namespace: String) = SymbolName(name.removePrefix(namespace), kind = kind)

    fun relative(namespace: Namespace) = relative(namespace.toString())

    fun relative(modulite: Modulite?): SymbolName {
        if (modulite == null) return this
        return relative(modulite.namespace)
    }

    /**
     * @return namespace of symbol or global namespace if symbol is not in any namespace.
     */
    fun namespace(): Namespace = split().first

    fun className(): SymbolName {
        return SymbolName(
            if (name.contains("::")) {
                name.substring(0, name.indexOf("::"))
            } else {
                name
            }
        )
    }

    fun splitMember(): Pair<SymbolName, SymbolName> {
        val lastColon = name.lastIndexOf("::")
        return if (lastColon == -1) {
            SymbolName(name, kind) to SymbolName("")
        } else {
            SymbolName(name.substring(0, lastColon), kind = Kind.Class) to SymbolName(name.substring(lastColon + 2), kind = kind)
        }
    }

    fun splitComposerPackageModulite(): Pair<SymbolName, SymbolName> {
        val lastSlash = name.lastIndexOf("/")
        return if (lastSlash == -1) {
            SymbolName(name, kind = Kind.ComposerPackageModulite) to SymbolName("")
        } else {
            SymbolName(name.substring(0, lastSlash), kind = Kind.ComposerPackage) to
                    SymbolName(name.substring(lastSlash + 1), kind = Kind.Modulite)
        }
    }

    fun isClassMember(): Boolean = kind == Kind.Field || kind == Kind.Method || kind == Kind.ClassConstant

    fun isGlobal(): Boolean {
        if (kind == Kind.GlobalVariable) return true
        if (kind == Kind.Modulite || kind == Kind.ComposerPackage || kind == Kind.ComposerPackageModulite) return false

        val (namespace, _) = split()
        return namespace.isGlobal()
    }

    /**
     * @return absolute name of symbol relative to the passed [modulite].
     */
    fun absolutize(modulite: ModuliteBase): SymbolName {
        return absolutize(modulite.namespace)
    }

    /**
     * @return absolute name of symbol relative to the passed [namespace].
     */
    fun absolutize(namespace: Namespace): SymbolName {
        if (isAbsolute()) {
            return this
        }
        // Для глобальных переменных всегда считаем пространство имен как \.
        if (kind == Kind.GlobalVariable) {
            return SymbolName(name)
        }

        if (namespace.isGlobal()) {
            return SymbolName("\\" + name)
        }

        return SymbolName(namespace.toString() + name)
    }

    fun resolve(project: Project): List<PsiNamedElement> {
        return when (kind) {
            Kind.Modulite                               -> {
                val namePsi = ModuliteIndex.getInstance(project).getModulite(name)
                    ?.namePsi() ?: return emptyList()

                listOf(namePsi)
            }

            Kind.ComposerPackageModulite                -> {
                val (composerPackageName, moduliteName) = splitComposerPackageModulite()
                val namePsi = ModuliteIndex.getInstance(project).getModulite(moduliteName.name, composerPackageName)
                    ?.namePsi() ?: return emptyList()

                listOf(namePsi)
            }

            Kind.ComposerPackage                        -> {
                val namePsi = ComposerPackagesIndex.getInstance(project).getPackage(name)
                    ?.namePsi() ?: return emptyList()

                listOf(namePsi)
            }

            Kind.Class                                  -> {
                PhpIndex.getInstance(project).getAnyByFQN(phpstormName())
            }

            Kind.Field, Kind.Method, Kind.ClassConstant -> {
                val (className, memberName) = splitMember()

                val classes = PhpIndex.getInstance(project).getAnyByFQN(className.toString())
                if (classes.isEmpty()) return emptyList()

                classes.mapNotNull { klass ->
                    when (kind) {
                        Kind.Field         -> klass.fields.find {
                            it.name == memberName.toString().removePrefix("$")
                        }

                        Kind.Method        -> klass.methods.find { it.name == memberName.toString().removeSuffix("()") }
                        Kind.ClassConstant -> klass.fields.find {
                            it.name == memberName.toString() && it.isConstant
                        }

                        else               -> null
                    }
                }
            }

            Kind.Function                               -> {
                PhpIndex.getInstance(project).getFunctionsByFQN(phpstormName())
            }

            Kind.GlobalVariable                         -> {
                PhpIndex.getInstance(project).getVariablesByName(variableName())
            }

            Kind.Constant                               -> {
                PhpIndex.getInstance(project).getConstantsByFQN(phpstormName())
            }

            else                                        -> return emptyList()
        }.toList()
    }

    fun equals(other: SymbolName, containingModulite: ModuliteBase): Boolean {
        if (other.isAbsolute()) {
            return other == this
        }

        return this == other.absolutize(containingModulite)
    }

    fun readableName(uppercase: Boolean = true, reversed: Boolean = false) = (when (kind) {
        Kind.Class          -> "Class"
        Kind.Field          -> "Property"
        Kind.Method         -> "Method"
        Kind.ClassConstant  -> "Class constant"
        Kind.Function       -> "Function"
        Kind.Constant       -> "Constant"
        Kind.GlobalVariable -> "Global variable"
        else                -> "unknown"
    }
        .applyIf(!uppercase) { lowercase() } + " " + name.removePrefix("\\"))
        .applyIf(reversed) { split(" ").reversed().joinToString(" ") }

    fun readableNameWithAction() = (when (kind) {
        Kind.Class          -> "use"
        Kind.Field          -> "use"
        Kind.Method         -> "call"
        Kind.ClassConstant  -> "use"
        Kind.Function       -> "call"
        Kind.Constant       -> "use"
        Kind.GlobalVariable -> "use global"
        else                -> "unknown"
    } + " " + name.removePrefix("\\"))

    fun kindReadableName(many: Boolean) = when (kind) {
        Kind.Modulite                -> if (many) "modulites" else "modulite"
        Kind.ComposerPackageModulite -> if (many) "modulites" else "modulite"
        Kind.ComposerPackage         -> if (many) "composer packages" else "composer package"
        Kind.Class                   -> if (many) "classes" else "class"
        Kind.Method                  -> if (many) "methods" else "method"
        Kind.Field                   -> if (many) "properties" else "property"
        Kind.ClassConstant           -> if (many) "class constants" else "class constant"
        Kind.Function                -> if (many) "functions" else "function"
        Kind.Constant                -> if (many) "constants" else "constant"
        Kind.GlobalVariable          -> if (many) "global variables" else "global variable"
        else                         -> "unknown"
    }

    fun toYaml(): String = name.replace("\\", "\\\\")

    override fun toString(): String = name

    override fun compareTo(other: SymbolName): Int {
        return name.compareTo(other.name)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SymbolName

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int = name.hashCode()

    private fun phpstormName(): String {
        return name.replace("::", ".").removeSuffix("()")
    }

    private fun variableName(): String {
        if (kind != Kind.GlobalVariable) return ""
        return name.removePrefix("\\").removePrefix("$")
    }

    fun isAbsolute(): Boolean = name.startsWith("\\") || kind == Kind.Modulite || kind == Kind.ComposerPackage

    private fun split(): Pair<Namespace, String> {
        val lastSlash = name.lastIndexOf("\\")
        return if (lastSlash == -1) {
            Pair(Namespace(), name)
        } else {
            Pair(Namespace(name.substring(0, lastSlash)), name.substring(lastSlash + 1))
        }
    }

    companion object {
        fun supposeKind(name: String): Kind {
            if (name.startsWith("@")) {
                return Kind.Modulite
            }

            if (name.startsWith("#")) {
                if (name.contains("@")) {
                    return Kind.ComposerPackageModulite
                }
                return Kind.ComposerPackage
            }

            if (name.contains("::")) {
                val memberName = name.split("::").last()
                // if $name
                if (memberName.startsWith("$")) {
                    return Kind.Field
                }
                // if name()
                val withBrackets = memberName.endsWith("()")
                if (withBrackets) {
                    return Kind.Method
                }
                // if others like CONST_NAME
                return Kind.ClassConstant
            }

            if (name.startsWith("$") || name.startsWith("\\$")) {
                return Kind.GlobalVariable
            }

            if (name.endsWith("()")) {
                return Kind.Function
            }

            // TODO: мы не можем тут понять чем является символ
            // константой или классом, так как у нас нет доступа к индексу.
            // Пока что не знаю как это сделать правильно.
            val lastPart = name.split('\\').last()
            if (lastPart == lastPart.uppercase() && lastPart.length > 3) {
                return Kind.Constant
            }

            return Kind.Class
        }

        private fun normalize(rawName: String) = rawName.replace("\\\\", "\\")
    }
}
