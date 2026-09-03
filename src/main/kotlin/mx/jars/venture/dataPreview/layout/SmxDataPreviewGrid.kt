package mx.jars.venture.dataPreview.layout

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta
import mx.jars.venture.dataPreview.model.SmxDataPreviewFilter
import mx.jars.venture.dataPreview.model.SmxDataPreviewSortDirection
import mx.jars.venture.dataPreview.model.SmxFieldOrdering
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonField
import mx.jars.venture.venture_core_connect.smxJson.TipoField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.JViewport
import javax.swing.event.ChangeEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.TableColumnModelEvent
import javax.swing.event.TableColumnModelListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableColumn
import kotlin.math.max
import kotlin.math.min

class SmxDataPreviewGrid(
    private val onSort: (String) -> Unit,
    private val onOpenRelation: (String) -> Unit,
    private val onApplyFilters: () -> Unit,
) {
    private val formatter = SmxDataPreviewFormatter()
    private val filterRow = SmxDataPreviewFilterRow(onApplyFilters)
    private val tableModel = object : DefaultTableModel() {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }
    private val table = JBTable(tableModel).apply {
        autoResizeMode = JBTable.AUTO_RESIZE_OFF
        rowSelectionAllowed = true
        columnSelectionAllowed = true
        tableHeader = null
    }
    private val gridHeader = SmxDataPreviewGridHeader(filterRow, onSort)
    private val headerViewport = JViewport().apply {
        isOpaque = false
        view = gridHeader
    }
    private val scrollPane = JBScrollPane(table).apply {
        setColumnHeaderView(null)
    }
    val component = JPanel(BorderLayout()).apply {
        isOpaque = false
        add(headerViewport, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }
    val preferredFocusedComponent = table

    private var currentRows: List<Map<String, SmxJsonField>> = emptyList()
    private var currentColumns: List<String> = emptyList()
    private var currentFieldMeta: Map<String, SmxDataPreviewFieldMeta> = emptyMap()
    private var currentOrderField: String? = null
    private var currentOrderDirection = SmxDataPreviewSortDirection.ASC
    private var hideRela = false
    private val hiddenColumns = linkedMapOf<String, TableColumn>()
    private var rebuildingTable = false

    init {
        table.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) = maybeShowPopup(event)
            override fun mouseReleased(event: MouseEvent) = maybeShowPopup(event)
        })
        table.columnModel.addColumnModelListener(object : TableColumnModelListener {
            override fun columnAdded(e: TableColumnModelEvent) = updateFilterColumns()
            override fun columnRemoved(e: TableColumnModelEvent) = updateFilterColumns()
            override fun columnMoved(e: TableColumnModelEvent) = updateFilterColumns()
            override fun columnMarginChanged(e: ChangeEvent) = updateFilterColumns()
            override fun columnSelectionChanged(e: ListSelectionEvent) {}
        })
        scrollPane.horizontalScrollBar.model.addChangeListener {
            syncHeaderScroll()
        }
        installHeaderPanel()
    }

    fun setRows(
        rows: List<Map<String, SmxJsonField>>,
        fieldMeta: Map<String, SmxDataPreviewFieldMeta>,
        orderField: String?,
        orderDirection: SmxDataPreviewSortDirection,
        hideRela: Boolean,
    ) {
        currentRows = rows
        currentFieldMeta = fieldMeta
        currentOrderField = orderField
        currentOrderDirection = orderDirection
        this.hideRela = hideRela
        render()
    }

    fun setHideRela(value: Boolean) {
        hideRela = value
        applyColumnVisibility()
        installColumnRenderers()
        fitColumnsToContent()
        updateFilterColumns()
        installHeaderPanel()
    }

    fun filters(): List<SmxDataPreviewFilter> =
        filterRow.filters()

    fun clearFilters() {
        filterRow.clear()
    }

    private fun render() {
        val columns = SmxFieldOrdering.sort((currentRows.firstOrNull()
            ?.keys
            ?: currentFieldMeta.keys)
            .toList()) { it }
        currentColumns = columns
        hiddenColumns.clear()

        rebuildingTable = true
        try {
            tableModel.setColumnCount(0)
            tableModel.setRowCount(0)

            columns.forEach { tableModel.addColumn(headerTitle(it)) }

            currentRows.forEach { row ->
                tableModel.addRow(columns.map { column ->
                    formatter.formatCell(column, row[column], currentFieldMeta)
                }.toTypedArray())
            }
        } finally {
            rebuildingTable = false
        }

        applyColumnVisibility()
        installColumnRenderers()
        fitColumnsToContent()
        updateFilterColumns()
        installHeaderPanel()
    }

    private fun installColumnRenderers() {
        for (viewIndex in 0 until table.columnCount) {
            val tableColumn = table.columnModel.getColumn(viewIndex)
            val column = currentColumns.getOrNull(tableColumn.modelIndex) ?: continue
            val meta = currentFieldMeta[column.uppercase()]
            val fields = currentRows.mapNotNull { it[column] }
            val alignRight = meta?.isNumeric() == true ||
                meta?.isDateOnly() == true ||
                meta?.isTimestamp() == true ||
                fields.any { it.valDate != null || it.tipoValor in RIGHT_ALIGNED_JSON_TYPES }
            tableColumn.cellRenderer = SmxDataPreviewCellRenderer(
                fieldName = column,
                alignRight = alignRight,
            )
        }
    }

    private fun maybeShowPopup(event: MouseEvent) {
        if (!event.isPopupTrigger) return
        val columnName = columnNameAt(event) ?: return
        if (!SmxDataPreviewFieldStyle.isRelationField(columnName)) return

        val group = DefaultActionGroup().apply {
            add(object : AnAction("Ver datos", "Abrir datos relacionados de $columnName", AllIcons.Actions.Preview) {
                override fun actionPerformed(e: AnActionEvent) {
                    onOpenRelation(columnName)
                }
            })
        }

        ActionManager.getInstance()
            .createActionPopupMenu(ActionPlaces.POPUP, group)
            .component
            .show(table, event.x, event.y)
    }

    private fun columnNameAt(event: MouseEvent): String? {
        val viewColumn = table.columnAtPoint(event.point)
        if (viewColumn < 0) return null
        val modelColumn = table.convertColumnIndexToModel(viewColumn)
        return currentColumns.getOrNull(modelColumn)
    }

    private fun headerTitle(column: String): String =
        if (column.equals(currentOrderField, ignoreCase = true)) {
            column + currentOrderDirection.marker
        } else {
            column
        }

    private fun fitColumnsToContent() {
        val metrics = table.getFontMetrics(table.font)
        val headerMetrics = gridHeader.getFontMetrics(gridHeader.font)

        for (columnIndex in 0 until table.columnCount) {
            val column = table.columnModel.getColumn(columnIndex)
            val header = column.headerValue?.toString().orEmpty()
            var width = headerMetrics.stringWidth(header) + COLUMN_PADDING

            for (rowIndex in 0 until table.rowCount) {
                val text = table.getValueAt(rowIndex, columnIndex)?.toString().orEmpty()
                width = max(width, metrics.stringWidth(text) + COLUMN_PADDING)
            }

            column.preferredWidth = min(max(width, MIN_COLUMN_WIDTH), MAX_COLUMN_WIDTH)
        }
    }

    private fun updateFilterColumns() {
        if (rebuildingTable) return

        if (currentColumns.isEmpty() || table.columnCount == 0) {
            return
        }

        val columns = (0 until table.columnCount).mapNotNull { viewIndex ->
            val modelIndex = table.convertColumnIndexToModel(viewIndex)
            val name = currentColumns.getOrNull(modelIndex) ?: return@mapNotNull null
            val width = table.columnModel.getColumn(viewIndex).width
                .takeIf { it > 0 }
                ?: table.columnModel.getColumn(viewIndex).preferredWidth
            SmxDataPreviewFilterColumn(name, width)
        }

        gridHeader.setColumns(columns, currentOrderField, currentOrderDirection)
    }

    private fun applyColumnVisibility() {
        if (currentColumns.isEmpty()) return

        if (hideRela) {
            for (viewIndex in table.columnCount - 1 downTo 0) {
                val tableColumn = table.columnModel.getColumn(viewIndex)
                val name = currentColumns.getOrNull(tableColumn.modelIndex) ?: continue
                if (SmxDataPreviewFieldStyle.isRelationField(name)) {
                    hiddenColumns[name] = tableColumn
                    table.columnModel.removeColumn(tableColumn)
                }
            }
            return
        }

        if (hiddenColumns.isEmpty()) return
        hiddenColumns.values.forEach { table.columnModel.addColumn(it) }
        hiddenColumns.clear()
        restoreColumnOrder()
    }

    private fun restoreColumnOrder() {
        currentColumns.forEachIndexed { targetIndex, columnName ->
            val viewIndex = findViewIndex(columnName) ?: return@forEachIndexed
            val boundedTarget = targetIndex.coerceAtMost(table.columnCount - 1)
            if (viewIndex != boundedTarget) {
                table.columnModel.moveColumn(viewIndex, boundedTarget)
            }
        }
    }

    private fun findViewIndex(columnName: String): Int? {
        for (viewIndex in 0 until table.columnCount) {
            val modelIndex = table.columnModel.getColumn(viewIndex).modelIndex
            if (currentColumns.getOrNull(modelIndex).equals(columnName, ignoreCase = true)) {
                return viewIndex
            }
        }
        return null
    }

    private fun installHeaderPanel() {
        gridHeader.syncSize(table.columnModel.totalColumnWidth)
        table.tableHeader = null
        scrollPane.setColumnHeaderView(null)
        headerViewport.preferredSize = Dimension(0, gridHeader.preferredSize.height)
        syncHeaderScroll()
        gridHeader.revalidate()
        gridHeader.repaint()
        headerViewport.revalidate()
        headerViewport.repaint()
        component.revalidate()
        component.repaint()
        SwingUtilities.invokeLater {
            table.tableHeader = null
            scrollPane.setColumnHeaderView(null)
            headerViewport.preferredSize = Dimension(0, gridHeader.preferredSize.height)
            syncHeaderScroll()
            gridHeader.revalidate()
            gridHeader.repaint()
            headerViewport.revalidate()
            headerViewport.repaint()
            component.revalidate()
            component.repaint()
        }
    }

    private fun syncHeaderScroll() {
        headerViewport.viewPosition = Point(scrollPane.horizontalScrollBar.value, 0)
    }

    companion object {
        private const val MIN_COLUMN_WIDTH = 90
        private const val MAX_COLUMN_WIDTH = 360
        private const val COLUMN_PADDING = 28
        private val RIGHT_ALIGNED_JSON_TYPES = setOf(
            TipoField.INT.toJson(),
            TipoField.LNG.toJson(),
            TipoField.DBL.toJson(),
        )
    }
}
