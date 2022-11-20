package com.vk.modulite.psi

import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor

open class PhpRecursiveElementVisitor : PhpElementVisitor() {
    override fun visitElement(element: PsiElement) {
        var child = element.firstChild
        while (child != null) {
            child.accept(this)
            child = child.nextSibling
        }
    }
}
