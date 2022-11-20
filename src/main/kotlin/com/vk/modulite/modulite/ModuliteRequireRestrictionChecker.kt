package com.vk.modulite.modulite

object ModuliteRequireRestrictionChecker {
    enum class ViolationTypes {
        Ok,
        RequireSelf,
        NotPublic,
    }

    fun canUse(current: Modulite, other: Modulite): Pair<Boolean, ViolationTypes> {
        if (current == other) {
            return result(ViolationTypes.RequireSelf)
        }

        if (!current.canUse(other)) {
            return result(ViolationTypes.NotPublic)
        }

        return result(ViolationTypes.Ok)
    }

    private fun result(type: ViolationTypes) = Pair(type == ViolationTypes.Ok, type)
}
