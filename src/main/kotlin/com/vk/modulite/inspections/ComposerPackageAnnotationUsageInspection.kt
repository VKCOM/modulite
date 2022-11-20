package com.vk.modulite.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.vk.modulite.utils.registerModuliteProblem

class ComposerPackageAnnotationUsageInspection : LocalInspectionTool() {
    class ComposerPackageAnnotationUsageQuickFix : LocalQuickFix {
        override fun getFamilyName() = "Remove annotation"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val tag = descriptor.psiElement
            removeTagFromDocComment(tag as PhpDocTag)
        }

        private fun removeTagFromDocComment(docTag: PhpDocTag) {
            val docComment = docTag.parentDocComment ?: return
            docComment.removeTag(docTag)
        }

        /**
         * Having a child inside doc comment (this), traverse psi tree up until the doc comment
         */
        private inline val PsiElement.parentDocComment: PhpDocComment?
            get() = PsiTreeUtil.getParentOfType(this, PhpDocComment::class.java)

        /**
         * Removes @tag from doc comment.
         * If it contains nothing but '@tag', removes doc comment itself.
         */
        private fun PhpDocComment.removeTag(docTag: PhpDocTag) {
            val asterisk = PsiTreeUtil.skipWhitespacesBackward(docTag)
                ?.takeIf { it.elementType == PhpTokenTypes.DOC_LEADING_ASTERISK }
            docTag.delete()
            asterisk?.delete()

            var child = firstChild.nextSibling
            while (child.elementType == PhpTokenTypes.DOC_LEADING_ASTERISK || child is PsiWhiteSpace)
                child = child.nextSibling
            if (child.elementType == PhpTokenTypes.DOC_COMMENT_END)
                delete()
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitPhpDocTag(tag: PhpDocTag) {
                if (tag.name == "@internal" || tag.name == "@package") {
                    holder.registerModuliteProblem(
                        tag,
                        "Usage of @internal or @package annotation is meaningless in a modulite",
                        ProblemHighlightType.WARNING,
                        ComposerPackageAnnotationUsageQuickFix()
                    )
                }
            }
        }
    }
}
