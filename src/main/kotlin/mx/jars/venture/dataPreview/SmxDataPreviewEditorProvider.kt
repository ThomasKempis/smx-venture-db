package mx.jars.venture.dataPreview

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SmxDataPreviewEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean =
        file is SmxDataPreviewVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        SmxDataPreviewEditor(project, file as SmxDataPreviewVirtualFile)

    override fun getEditorTypeId(): String = "smx-data-preview-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS
}
