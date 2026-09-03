package mx.jars.venture.source

import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

class SmxObjectSourceVirtualFile(
    val objectType: String,
    val objectName: String,
    val sourceCode: String,
) : LightVirtualFile(
    "${objectName.trim().ifBlank { "source" }}.${if (objectType.uppercase() == "VIEW") "view" else "sql"}",
    PlainTextFileType.INSTANCE,
    sourceCode,
) {
    init {
        isWritable = false
        setWritable(false)
    }

    override fun getPath(): String = name

    override fun getParent(): VirtualFile? = null
}
