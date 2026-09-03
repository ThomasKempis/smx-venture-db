package mx.jars.venture.dataPreview.layout

import com.intellij.ui.JBColor
import java.awt.Color

object SmxDataPreviewFieldStyle {
    private val relationColor = JBColor(0xB05A00, 0xF0A45D)
    private val idColor = JBColor(0x2E7D32, 0x89D185)

    fun foregroundForField(fieldName: String, selected: Boolean): Color? {
        if (selected) return null
        val normalized = fieldName.uppercase()
        return when {
            normalized.startsWith("RELA_") -> relationColor
            normalized.startsWith("ID_") || normalized.endsWith("_FAPL") -> idColor
            else -> null
        }
    }

    fun isBoldField(fieldName: String): Boolean {
        val normalized = fieldName.uppercase()
        return normalized.startsWith("ID_") || normalized.endsWith("_FAPL")
    }

    fun isRelationField(fieldName: String): Boolean =
        fieldName.uppercase().startsWith("RELA_")
}
