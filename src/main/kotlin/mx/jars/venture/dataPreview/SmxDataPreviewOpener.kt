package mx.jars.venture.dataPreview

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

object SmxDataPreviewOpener {
    fun open(project: Project, objectType: String, objectName: String) {
        val request = SmxDataPreviewRequest(objectType, objectName)
        val file = SmxDataPreviewVirtualFile(request)
        FileEditorManager.getInstance(project).openFile(file, true)
    }
}
