package mx.jars.venture.source

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class SmxObjectSourceEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file is SmxObjectSourceVirtualFile

    override fun createEditor(project: Project, file: VirtualFile): FileEditor =
        SmxObjectSourceEditor(file as SmxObjectSourceVirtualFile)

    override fun getEditorTypeId(): String = "smx-object-source-editor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS
}
