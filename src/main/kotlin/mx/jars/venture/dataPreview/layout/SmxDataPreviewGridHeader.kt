package mx.jars.venture.dataPreview.layout

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import mx.jars.venture.dataPreview.model.SmxDataPreviewSortDirection
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Point
import javax.swing.BoxLayout
import javax.swing.JPanel

class SmxDataPreviewGridHeader(
    private val filterRow: SmxDataPreviewFilterRow,
    private val onSort: (String) -> Unit,
) : JPanel() {
    private val headerRow = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        isOpaque = true
        background = JBColor.PanelBackground
    }
    private var columnSignature = ""
    private var currentColumns: List<SmxDataPreviewFilterColumn> = emptyList()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        add(headerRow)
        add(filterRow)
    }

    fun setColumns(
        columns: List<SmxDataPreviewFilterColumn>,
        orderField: String?,
        orderDirection: SmxDataPreviewSortDirection,
        force: Boolean = false,
    ) {
        val signature = columns.joinToString("|") { it.name } +
            "|${orderField.orEmpty()}|${orderDirection.name}"
        currentColumns = columns

        if (force || signature != columnSignature) {
            columnSignature = signature
            rebuildHeaderRow(columns, orderField, orderDirection)
            filterRow.setColumns(columns)
        } else {
            resizeHeaderRow(columns)
            filterRow.resizeColumns(columns)
        }

        syncSize(columns.sumOf { it.width })
    }

    fun columnNameAt(point: Point): String? {
        var x = 0
        currentColumns.forEach { column ->
            x += column.width
            if (point.x < x) return column.name
        }
        return null
    }

    fun syncSize(totalColumnWidth: Int) {
        preferredSize = Dimension(
            totalColumnWidth,
            HEADER_HEIGHT + filterRow.preferredSize.height,
        )
        minimumSize = Dimension(0, preferredSize.height)
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
        revalidate()
        repaint()
    }

    private fun rebuildHeaderRow(
        columns: List<SmxDataPreviewFilterColumn>,
        orderField: String?,
        orderDirection: SmxDataPreviewSortDirection,
    ) {
        headerRow.removeAll()
        columns.forEach { column ->
            headerRow.add(newHeaderLabel(column, orderField, orderDirection))
        }
        resizeHeaderRow(columns)
    }

    private fun resizeHeaderRow(columns: List<SmxDataPreviewFilterColumn>) {
        columns.forEachIndexed { index, column ->
            val label = headerRow.getComponent(index) ?: return@forEachIndexed
            val size = Dimension(column.width, HEADER_HEIGHT)
            label.preferredSize = size
            label.minimumSize = size
            label.maximumSize = size
        }

        val width = columns.sumOf { it.width }
        headerRow.preferredSize = Dimension(width, HEADER_HEIGHT)
        headerRow.minimumSize = Dimension(0, HEADER_HEIGHT)
        headerRow.maximumSize = Dimension(Int.MAX_VALUE, HEADER_HEIGHT)
        headerRow.revalidate()
        headerRow.repaint()
    }

    private fun newHeaderLabel(
        column: SmxDataPreviewFilterColumn,
        orderField: String?,
        orderDirection: SmxDataPreviewSortDirection,
    ): JBLabel =
        JBLabel(title(column.name, orderField, orderDirection)).apply {
            border = JBUI.Borders.empty(0, 8)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            preferredSize = Dimension(column.width, HEADER_HEIGHT)
            minimumSize = Dimension(column.width, HEADER_HEIGHT)
            maximumSize = Dimension(column.width, HEADER_HEIGHT)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    onSort(column.name)
                }
            })
        }

    private fun title(
        column: String,
        orderField: String?,
        orderDirection: SmxDataPreviewSortDirection,
    ): String =
        if (column.equals(orderField, ignoreCase = true)) {
            column + orderDirection.marker
        } else {
            column
        }

    companion object {
        private const val HEADER_HEIGHT = 30
    }
}
