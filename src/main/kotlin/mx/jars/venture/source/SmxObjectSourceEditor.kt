package mx.jars.venture.source

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.util.Key
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class SmxObjectSourceEditor(
    private val file: SmxObjectSourceVirtualFile,
) : FileEditor {
    private val editorPanel = SmxSqlCodeEditorPanel(file.sourceCode)

    override fun getComponent(): JComponent = editorPanel.getEditorComponent()

    override fun getPreferredFocusedComponent(): JComponent = editorPanel.getPreferredFocusedComponent()

    override fun getName(): String = file.name

    override fun getFile(): SmxObjectSourceVirtualFile = file

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = editorPanel.isModified()

    override fun isValid(): Boolean = true

    override fun selectNotify() = Unit

    override fun deselectNotify() = Unit

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun dispose() = editorPanel.dispose()

    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE

    override fun <T : Any?> getUserData(key: Key<T>): T? = null

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) = Unit
}
