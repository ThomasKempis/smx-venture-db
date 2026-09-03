package mx.jars.venture.source

import com.intellij.openapi.fileEditor.impl.EditorTabColorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Color

class SmxEditorTabColorProvider : EditorTabColorProvider {
    override fun getEditorTabColor(project: Project, file: VirtualFile): Color? =
        when {
            file is SmxObjectSourceVirtualFile && file.extension.equals("view", ignoreCase = true) -> VIEW_COLOR
            file is SmxObjectSourceVirtualFile -> CODE_COLOR
            file.name.endsWith(".data", ignoreCase = true) -> DATA_COLOR
            else -> null
        }

    companion object {
        private val DATA_COLOR = JBColor(0xF8D7DA, 0x6E2C32)
        private val VIEW_COLOR = JBColor(0xDDF3E4, 0x28543A)
        private val CODE_COLOR = JBColor(0xE9DDF5, 0x50366B)
    }
}
