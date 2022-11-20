package com.vk.modulite.inspections

import com.intellij.codeInspection.*
import com.intellij.ide.actions.QualifiedNameProviderUtil.getQualifiedName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.lexer.PhpTokenTypes
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.PhpPsiElementFactory
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.PhpNamespace
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor
import com.vk.modulite.Namespace
import com.vk.modulite.psi.extensions.files.containingModulite
import com.vk.modulite.utils.registerModuliteProblem

class WrongNamespaceInspection : LocalInspectionTool() {
    class InsertNamespaceQuickFix(private val namespace: String) : LocalQuickFix {
        override fun getFamilyName() = "Insert $namespace"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val phpOpeningTag = descriptor.psiElement
            val modulite = phpOpeningTag.containingModulite() ?: return
            val namespace = modulite.namespace.toPHP()
            val namespaceFile = PhpPsiElementFactory.createFromText(project, PhpNamespace::class.java, "namespace $namespace;")
                ?: return
            phpOpeningTag.parent.addRangeAfter(namespaceFile.prevSibling, namespaceFile, phpOpeningTag)
        }
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : PhpElementVisitor() {
            override fun visitFile(file: PsiFile) {
                if (file !is PhpFile) {
                    return
                }

                val modulite = file.containingModulite() ?: return
                if (file.mainNamespaceName == null) return
                if (file.mainNamespaceName.isNotEmpty()) return
                if (modulite.namespace.isGlobal()) return

                val groupStatement = file.getFirstChild()
                val phpOpeningTag = PhpPsiUtil.getChildOfType(groupStatement, PhpTokenTypes.PHP_OPENING_TAG) ?: return
                val expectedNs = modulite.namespace.toPHP()

                holder.registerModuliteProblem(
                    phpOpeningTag,
                    "Please specify a namespace for this file ($expectedNs or its subnamespace)",
                    ProblemHighlightType.GENERIC_ERROR,
                    InsertNamespaceQuickFix(expectedNs),
                )
            }

            override fun visitPhpNamespace(namespacePsi: PhpNamespace) {
                val modulite = namespacePsi.containingModulite() ?: return
                val identifier = namespacePsi.nameIdentifier ?: return
                val namespace = Namespace(getQualifiedName(namespacePsi)?: return)

                if (!modulite.isValidNamespace(namespace)) {
                    val actualNs = namespace.toPHP()
                    val expectedNs = modulite.namespace.toPHP()

                    holder.registerModuliteProblem(
                        identifier,
                        "Namespace $actualNs is not valid for $modulite, namespace must be $expectedNs or its subnamespace",
                        ProblemHighlightType.GENERIC_ERROR,
                    )
                }
            }
        }
    }
}
