package mx.jars.venture.dataPreview

import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

class SmxDataPreviewVirtualFile(
    val request: SmxDataPreviewRequest,
) : LightVirtualFile("${request.objectName}.data", UnknownFileType.INSTANCE, "") {
    init {
        isWritable = false
        setWritable(false)
    }

    override fun getPath(): String = name

    override fun getParent(): VirtualFile? = null
}
