package com.vk.modulite.inspections.intentions

import com.jetbrains.php.lang.psi.elements.*
import com.jetbrains.php.lang.psi.elements.Function
import com.jetbrains.php.lang.psi.elements.impl.PhpDefineImpl
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

abstract class BaseIntentionPhpVisitor : PhpElementVisitor() {
    override fun visitPhpClass(klass: PhpClass) {
        addProblem(klass)
    }

    override fun visitPhpMethod(method: Method) {
        // Не добавляем intention если метод класса приватный.
        if (!method.modifier.isPublic) {
            return
        }

        // Не добавляем intention, если метод класса не статичный.
        if (!method.modifier.isStatic) {
            return
        }

        addProblem(method)
    }

    override fun visitPhpFunction(function: Function) {
        addProblem(function)
    }

    override fun visitPhpConstant(constant: Constant) {
        addProblem(constant)
    }

    override fun visitPhpClassFieldsList(list: PhpClassFieldsList) {
        list.fields.forEach {
            // Не добавляем intention если член класса приватный.
            if (!it.modifier.isPublic) {
                return
            }

            // Не добавляем intention, если член класса не статичный.
            if (!it.modifier.isStatic) {
                return
            }

            addProblem(it)
        }
    }

    override fun visitPhpFunctionCall(reference: FunctionReference) {
        if (reference.name == "define") {
            val define = reference.parent as PhpDefineImpl
            addProblem(define)
        }
    }

    abstract fun addProblem(namedElement: PhpNamedElement)
}
