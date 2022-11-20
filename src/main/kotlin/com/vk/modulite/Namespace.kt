package com.vk.modulite

/**
 * Represent namespace of some PHP symbol.
 *
 * Store namespace in `Name\\Other` form.
 * If namespace is global, store it as empty string.
 *
 * [toString] method return common namespace view as `\\Name\\Other\\` or `\\` for global.
 *
 * [toYaml] method return namespace view for YAML config as `Name\\Other\\` or `\\` for global.
 *
 * [toPHP] method return namespace view for PHP inspections as `Name\\Other` or `<global namespace>` for global.
 */
class Namespace(name: String = "") {
    private val name = normalize(name)

    /**
     * @return `true` if namespace is global, `false` otherwise.
     */
    fun isGlobal() = name.isEmpty()

    /**
     * @return `true` if this namespace is a subnamespace of [other], `false` otherwise.
     *
     * E.g. `\\Name\\Other\\` is a subnamespace of `\\Name\\`
     */
    fun subOf(other: Namespace) = toString().startsWith(other.toString())

    fun toPHP(): String = if (isGlobal()) "<global namespace>" else name

    fun toYaml(): String = toString().removePrefix("\\").replace("\\", "\\\\")

    override fun toString(): String = if (isGlobal()) "\\" else "\\$name\\"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Namespace

        if (name != other.name) return false

        return true
    }

    override fun hashCode() = name.hashCode()

    // Helper methods.

    fun last(): String = name.split("\\").last()

    fun replaceLast(new: String): Namespace {
        val parts = name.split("\\").toMutableList()
        parts[parts.size - 1] = new
        return Namespace(parts.joinToString("\\"))
    }

    companion object {
        private fun normalize(name: String) = name
            .replace("\\\\", "\\")
            .trim('\\')
    }
}
