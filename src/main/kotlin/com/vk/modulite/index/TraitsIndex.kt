package com.vk.modulite.index

import com.intellij.openapi.components.Service
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.intellij.psi.util.PsiTreeUtil

@Service(Service.Level.PROJECT)
class TraitsIndex : FileBasedIndexExtension<String, String>() {
    companion object {
        val KEY = ID.create<String, String>("traits.index")
    }

    override fun getIndexer(): DataIndexer<String, String, FileContent> {
        return DataIndexer { inputData ->
            val map = mutableMapOf<String, String>()
            val psiFile = inputData.psiFile

            if (psiFile is PhpFile) {
                val classes = PsiTreeUtil.findChildrenOfType(psiFile, PhpClass::class.java)
                for (klass in classes) {
                    if (klass.isTrait) {
                        map[klass.fqn] = psiFile.virtualFile.path
                    }
                }
            }
            map
        }
    }

    override fun getName(): ID<String, String> = KEY

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): EnumeratorStringDescriptor = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return DefaultFileTypeSpecificInputFilter(PhpFileType.INSTANCE)
    }

    override fun dependsOnFileContent() = true

    override fun getVersion(): Int = 1
}
