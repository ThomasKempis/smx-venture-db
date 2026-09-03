package mx.jars.venture.dataPreview

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.components.JBPanel
import mx.jars.venture.dataPreview.backend.SmxAliasResolver
import mx.jars.venture.dataPreview.backend.SmxIdFieldResolver
import mx.jars.venture.dataPreview.backend.SmxRelationResolver
import mx.jars.venture.services.SmxPreviewQueryOptions
import mx.jars.venture.services.SmxPreviewService
import mx.jars.venture.dataPreview.layout.SmxDataPreviewGrid
import mx.jars.venture.dataPreview.layout.SmxDataPreviewToolbar
import mx.jars.venture.dataPreview.layout.SmxProcedureParameterPanel
import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta
import mx.jars.venture.dataPreview.model.SmxDataPreviewFilter
import mx.jars.venture.dataPreview.model.SmxDataPreviewPage
import mx.jars.venture.dataPreview.model.SmxDataPreviewSortDirection
import mx.jars.venture.dataPreview.model.SmxDictionaryObject
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonField
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import kotlin.collections.filter

class SmxDataPreviewEditor(
    private val project: Project,
    private val file: SmxDataPreviewVirtualFile,
) : UserDataHolderBase(), FileEditor {
    private val previewService = SmxPreviewService()
    private val aliasResolver = SmxAliasResolver()
    private val idFieldResolver = SmxIdFieldResolver(aliasResolver)
    private val relationResolver = SmxRelationResolver(aliasResolver)
    private val request = file.request

    private var activePage = 1
    private var totalPages = 0
    private var totalRows = 0
    private var fieldMeta: Map<String, SmxDataPreviewFieldMeta> = emptyMap()
    private var orderField: String? = null
    private var orderDirection = SmxDataPreviewSortDirection.ASC
    private var activeFilters: List<SmxDataPreviewFilter> = emptyList()
    private var currentRows: List<Map<String, SmxJsonField>> = emptyList()
    private var dictionaryObjects: List<SmxDictionaryObject> = emptyList()

    private val toolbar = SmxDataPreviewToolbar(
        onFirst = {
            activePage = 1
            loadCurrentPage()
        },
        onPrevious = {
            activePage = (activePage - 1).coerceAtLeast(1)
            loadCurrentPage()
        },
        onNext = {
            activePage = (activePage + 1).coerceAtMost(totalPages.coerceAtLeast(1))
            loadCurrentPage()
        },
        onLast = {
            activePage = totalPages.coerceAtLeast(1)
            loadCurrentPage()
        },
        onHideRelaChanged = { hidden ->
            grid.setHideRela(hidden)
        },
        onApplyFilters = {
            applyFilters()
        },
        onClearFilters = {
            clearFilters()
        },
    )
    private val procedureParameterPanel = SmxProcedureParameterPanel {
        activePage = 1
        loadCurrentPage()
    }
    private val grid = SmxDataPreviewGrid(
        onSort = { columnName -> changeOrder(columnName) },
        onOpenRelation = { columnName -> openRelation(columnName) },
        onApplyFilters = { applyFilters() },
    )
    private val component = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        add(buildTopPanel(), BorderLayout.NORTH)
        add(grid.component, BorderLayout.CENTER)
    }

    init {
        if (isProcedure()) {
            loadProcedureSetup()
        } else {
            loadCurrentPage()
        }
    }

    override fun getComponent(): JComponent = component

    override fun getPreferredFocusedComponent(): JComponent = grid.preferredFocusedComponent

    override fun getName(): String = request.objectName

    override fun setState(state: FileEditorState) {}

    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun getFile() = file

    override fun dispose() {}

    private fun buildTopPanel(): JComponent =
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(toolbar, BorderLayout.NORTH)
            add(procedureParameterPanel, BorderLayout.CENTER)
        }

    private fun ensureFieldMeta() {
        if (fieldMeta.isEmpty()) {
            fieldMeta = previewService.loadFieldMeta(request.objectType, request.objectName)
        }
        if (!isProcedure() && orderField.isNullOrBlank()) {
            val idField = idFieldResolver.resolveFromFields(
                request.objectType,
                request.objectName,
                fieldMeta.keys,
            )
                ?: return
            orderField = fieldMeta.keys.firstOrNull { it.equals(idField, ignoreCase = true) }
        }
    }

    private fun loadProcedureSetup() {
        setLoading(true)
        object : SwingWorker<Map<String, SmxDataPreviewFieldMeta>, Unit>() {
            override fun doInBackground(): Map<String, SmxDataPreviewFieldMeta> {
                ensureFieldMeta()
                return fieldMeta
            }

            override fun done() {
                SwingUtilities.invokeLater {
                    runCatching {
                        fieldMeta = get()
                        val parameters = fieldMeta.values.filter { it.isParameter() }
                        procedureParameterPanel.setParameters(parameters)
                        toolbar.setStatus(if (parameters.isEmpty()) "Sin parámetros" else "Listo para ejecutar")
                        if (parameters.isEmpty()) {
                            loadCurrentPage()
                            return@invokeLater
                        }
                    }.onFailure { error ->
                        toolbar.setStatus(error.message ?: "No se pudieron cargar parámetros")
                    }
                    setLoading(false)
                }
            }
        }.execute()
    }

    private fun loadCurrentPage() {
        setLoading(true)
        object : SwingWorker<SmxDataPreviewPage, Unit>() {
            override fun doInBackground(): SmxDataPreviewPage {
                ensureFieldMeta()
                return SmxDataPreviewPage(
                    rowSet = previewService.loadPage(
                        request = request,
                        options = SmxPreviewQueryOptions(
                            orderField = orderField,
                            orderDirection = orderDirection,
                            page = activePage,
                            procedureParameters = procedureParameterPanel.values(),
                            fieldMeta = fieldMeta,
                            filters = activeFilters,
                        ),
                    ),
                )
            }

            override fun done() {
                SwingUtilities.invokeLater {
                    runCatching {
                        val page = get()
                        updateRows(page.rowSet)
                        updatePageState(page.rowSet)
                        if (isProcedure()) {
                            procedureParameterPanel.collapse()
                        }
                    }.onFailure { error ->
                        toolbar.setStatus(error.message ?: "No se pudieron cargar datos")
                    }
                    setLoading(false)
                }
            }
        }.execute()
    }

    private fun updateRows(rowSet: SmxRowSet) {
        currentRows = rowSet.getRows().map { it.row }
        grid.setRows(
            rows = currentRows,
            fieldMeta = fieldMeta,
            orderField = orderField,
            orderDirection = orderDirection,
            hideRela = toolbar.hideRela(),
        )
    }

    private fun updatePageState(rowSet: SmxRowSet) {
        val pageTotalRows = rowSet.getMaxRows()
        if (pageTotalRows > 0) {
            totalRows = pageTotalRows
        } else if (isProcedure()) {
            totalRows = rowSet.getRows().size
        }

        totalPages = calculateTotalPages(totalRows)
        if (totalPages > 0) {
            activePage = activePage.coerceIn(1, totalPages)
        }
        updateStatus()
    }

    private fun changeOrder(columnName: String) {
        val normalized = columnName.uppercase()
        orderDirection = if (orderField == normalized) {
            orderDirection.toggle()
        } else {
            SmxDataPreviewSortDirection.ASC
        }
        orderField = normalized
        activePage = 1
        loadCurrentPage()
    }

    private fun applyFilters() {
        activeFilters = grid.filters()
        activePage = 1
        loadCurrentPage()
    }

    private fun clearFilters() {
        grid.clearFilters()
        activeFilters = emptyList()
        activePage = 1
        loadCurrentPage()
    }

    private fun openRelation(columnName: String) {
        setLoading(true)
        toolbar.setStatus("Resolviendo relación...")

        object : SwingWorker<Unit, Unit>() {
            override fun doInBackground() {
                if (dictionaryObjects.isEmpty()) {
                    dictionaryObjects = previewService.listObjects()
                }
                val targets = relationResolver.resolve(columnName, dictionaryObjects)
                if (targets.isEmpty()) return

                SwingUtilities.invokeLater {
                    targets.forEach { target ->
                        SmxDataPreviewOpener.open(project, target.objectType, target.objectName)
                    }
                }
            }

            override fun done() {
                SwingUtilities.invokeLater {
                    runCatching {
                        get()
                    }.onFailure { error ->
                        toolbar.setStatus(error.message ?: "No se pudo abrir la relación")
                    }
                    setLoading(false)
                    updateStatus()
                }
            }
        }.execute()
    }

    private fun calculateTotalPages(rows: Int): Int =
        if (rows <= 0) 0 else ((rows + request.pageSize - 1) / request.pageSize)

    private fun updateStatus() {
        toolbar.setStatus(if (totalPages <= 0) "0/0" else "$activePage/$totalPages")
    }

    private fun setLoading(loading: Boolean) {
        toolbar.setBusy(loading, activePage, totalPages)
        procedureParameterPanel.setBusy(loading)
    }

    private fun isProcedure(): Boolean =
        request.objectType.equals("PROCEDURE", ignoreCase = true)
}
