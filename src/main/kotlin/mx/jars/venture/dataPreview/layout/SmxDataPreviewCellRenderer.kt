package mx.jars.venture.dataPreview.layout

import java.awt.Color
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class SmxDataPreviewCellRenderer(
    private val fieldName: String,
    private val alignRight: Boolean,
) : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        horizontalAlignment = if (alignRight) JLabel.RIGHT else JLabel.LEFT
        if (!isSelected) {
            component.background = columnBackground(table.background, column)
            foreground = SmxDataPreviewFieldStyle.foregroundForField(fieldName, selected = false) ?: table.foreground
            font = if (SmxDataPreviewFieldStyle.isBoldField(fieldName)) {
                table.font.deriveFont(java.awt.Font.BOLD)
            } else {
                table.font
            }
        } else {
            foreground = table.selectionForeground
            font = table.font
        }
        return component
    }

    private fun columnBackground(base: Color, column: Int): Color {
        if (column % 2 == 0) return base
        val delta = if (isDark(base)) 10 else -6
        return Color(
            (base.red + delta).coerceIn(0, 255),
            (base.green + delta).coerceIn(0, 255),
            (base.blue + delta).coerceIn(0, 255),
        )
    }

    private fun isDark(color: Color): Boolean =
        (color.red * 299 + color.green * 587 + color.blue * 114) / 1000 < 128
}
