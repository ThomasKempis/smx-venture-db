package mx.jars.venture.source

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

object SmxObjectSourceOpener {
    fun open(project: Project, objectType: String, objectName: String) {
        val source = SmxObjectSourceService().loadSource(objectType, objectName)
        val content = source.ifBlank { "-- No hay código disponible para este objeto." }
        val virtualFile = SmxObjectSourceVirtualFile(objectType, objectName, content)
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }
}
