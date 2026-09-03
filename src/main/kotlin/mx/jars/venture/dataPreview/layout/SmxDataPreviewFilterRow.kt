package mx.jars.venture.dataPreview.layout

import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import mx.jars.venture.dataPreview.model.SmxDataPreviewFilter
import java.awt.Dimension
import javax.swing.BoxLayout

class SmxDataPreviewFilterRow(
    private val onSubmit: () -> Unit,
) : JBPanel<SmxDataPreviewFilterRow>() {
    private val inputs = linkedMapOf<String, JBTextField>()
    private val values = linkedMapOf<String, String>()

    init {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = false
        border = JBUI.Borders.empty(2, 0, 3, 0)
    }

    fun setColumns(columns: List<SmxDataPreviewFilterColumn>) {
        inputs.values.forEach { input ->
            val name = input.name
            if (!name.isNullOrBlank()) {
                values[name] = input.text.orEmpty()
            }
        }

        removeAll()
        inputs.clear()

        columns.forEach { column ->
            val input = JBTextField().apply {
                name = column.name
                emptyText.text = "Filtrar"
                text = values[column.name].orEmpty()
                border = JBUI.Borders.empty(2, 6)
                preferredSize = Dimension(column.width, FILTER_HEIGHT)
                minimumSize = Dimension(column.width, FILTER_HEIGHT)
                maximumSize = Dimension(column.width, FILTER_HEIGHT)
                addActionListener { onSubmit() }
            }
            inputs[column.name] = input
            add(input)
        }

        val width = columns.sumOf { it.width }
        val height = FILTER_HEIGHT + JBUI.scale(5)
        preferredSize = Dimension(width, height)
        minimumSize = Dimension(0, height)
        maximumSize = Dimension(Int.MAX_VALUE, height)

        revalidate()
        repaint()
    }

    fun resizeColumns(columns: List<SmxDataPreviewFilterColumn>) {
        columns.forEach { column ->
            val input = inputs[column.name] ?: return@forEach
            val size = Dimension(column.width, FILTER_HEIGHT)
            input.preferredSize = size
            input.minimumSize = size
            input.maximumSize = size
        }

        val width = columns.sumOf { it.width }
        val height = FILTER_HEIGHT + JBUI.scale(5)
        preferredSize = Dimension(width, height)
        minimumSize = Dimension(0, height)
        maximumSize = Dimension(Int.MAX_VALUE, height)

        revalidate()
        repaint()
    }

    fun filters(): List<SmxDataPreviewFilter> =
        inputs.mapNotNull { (fieldName, input) ->
            val value = input.text.orEmpty().trim()
            if (value.isBlank()) null else SmxDataPreviewFilter(fieldName, value)
        }

    fun clear() {
        values.clear()
        inputs.values.forEach { it.text = "" }
    }

    companion object {
        private const val FILTER_HEIGHT = 26
    }
}

data class SmxDataPreviewFilterColumn(
    val name: String,
    val width: Int,
)
