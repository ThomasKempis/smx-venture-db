package mx.jars.venture.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.SearchTextField
import com.intellij.ui.JBColor
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.openapi.project.Project
import mx.jars.venture.dataPreview.SmxDataPreviewOpener
import mx.jars.venture.dictionary.SmxDictionaryViewMode
import mx.jars.venture.services.SmxCatalogService
import mx.jars.venture.source.SmxObjectSourceOpener
import mx.jars.venture.dataPreview.model.SmxFieldOrdering
import mx.jars.venture.model.SmxDatabaseField
import mx.jars.venture.model.SmxDatabaseFieldSection
import mx.jars.venture.model.SmxDatabaseDependency
import mx.jars.venture.model.SmxDatabaseObject
import mx.jars.venture.model.SmxDatabaseObjectType
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.awt.FlowLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel

class SmxDictionaryTreePanel(
    private val project: Project,
) : JBPanel<SmxDictionaryTreePanel>(BorderLayout()) {
    private val catalogService = SmxCatalogService()
    private val rootNode = DefaultMutableTreeNode("Diccionario")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true
        isOpaque = false
        background = null
        cellRenderer = TransparentTreeCellRenderer()
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
    }
    private val searchField = SearchTextField().apply {
        textEditor.emptyText.text = "Buscar tabla, vista o procedimiento"
    }
    private val modeModel = DefaultComboBoxModel<SmxDictionaryViewMode>().apply {
        addElement(SmxDictionaryViewMode.ALL)
    }
    private val modeCombo = JComboBox(modeModel).apply {
        isVisible = false
    }
    private val headerPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        isOpaque = false
        add(searchField, BorderLayout.NORTH)
        add(modeCombo, BorderLayout.SOUTH)
    }
    private val scrollPane = JBScrollPane(tree).apply {
        isOpaque = false
        viewport.isOpaque = false
        border = null
    }
    private var allDictionaryItems: List<DictionaryTreeItem> = emptyList()
    private var dictionaryItems: List<DictionaryTreeItem> = emptyList()
    private val fieldCache: MutableMap<String, List<Any>> = mutableMapOf()
    private val loadingFieldKeys: MutableSet<String> = mutableSetOf()
    private var updatingMode = false

    init {
        isOpaque = false
        add(headerPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        searchField.textEditor.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = applyFilter()
            override fun removeUpdate(e: DocumentEvent) = applyFilter()
            override fun changedUpdate(e: DocumentEvent) = applyFilter()
        })
        modeCombo.addActionListener {
            if (!updatingMode) {
                applySelectedMode()
            }
        }
        tree.addTreeSelectionListener(FieldSelectionListener())
        tree.addMouseListener(DictionaryPopupListener())
        isVisible = false
    }

    fun loadDictionary() {
        isVisible = true
        searchField.text = ""
        searchField.textEditor.emptyText.text = "Cargando diccionario..."
        clearTree()

        object : SwingWorker<List<SmxDatabaseObject>, Unit>() {
            override fun doInBackground(): List<SmxDatabaseObject> =
                catalogService.loadObjects()

            override fun done() {
                SwingUtilities.invokeLater {
                    runCatching {
                        buildTree(get())
                    }.onFailure { error ->
                        searchField.textEditor.emptyText.text = error.message ?: "No se pudo cargar el diccionario"
                    }
                }
            }
        }.execute()
    }

    fun showDictionary(rowSet: SmxRowSet) {
        showDictionary(catalogService.mapCatalog(rowSet))
    }

    fun showDictionary(objects: List<SmxDatabaseObject>) {
        isVisible = true
        searchField.text = ""
        searchField.textEditor.emptyText.text = "Buscar tabla, vista o procedimiento"
        buildTree(objects)
    }

    fun clear() {
        allDictionaryItems = emptyList()
        dictionaryItems = emptyList()
        fieldCache.clear()
        loadingFieldKeys.clear()
        searchField.text = ""
        searchField.textEditor.emptyText.text = "Buscar tabla, vista o procedimiento"
        resetModes()
        clearTree()
        isVisible = false
    }

    private fun buildTree(objects: List<SmxDatabaseObject>) {
        val items = objects
            .filter { it.isSupported() }
            .map(::toTreeItem)
            .let(::withRootTotals)

        allDictionaryItems = items
        dictionaryItems = items
        fieldCache.clear()
        loadingFieldKeys.clear()
        searchField.textEditor.emptyText.text = "Buscar tabla, vista o procedimiento"
        resetModes()
        modeCombo.isVisible = true
        rebuildTree(items)
    }

    private fun toTreeItem(databaseObject: SmxDatabaseObject): DictionaryTreeItem =
        DictionaryTreeItem(
            name = databaseObject.displayName,
            type = databaseObject.type,
            id = databaseObject.nodeId,
            parentId = databaseObject.parentId,
            objectName = databaseObject.name,
        )

    private fun withRootTotals(items: List<DictionaryTreeItem>): List<DictionaryTreeItem> {
        val totalsByParentId = items
            .filter { it.parentId.isNotBlank() }
            .groupingBy { it.parentId }
            .eachCount()

        return items.map { item ->
            if (item.parentId.isBlank() && item.isRootGroup()) {
                item.copy(name = "${item.baseRootName()} (${totalsByParentId[item.id] ?: 0})")
            } else {
                item
            }
        }
    }

    private fun DictionaryTreeItem.isRootGroup(): Boolean =
        parentId.isBlank() && type != SmxDatabaseObjectType.UNKNOWN

    private fun DictionaryTreeItem.baseRootName(): String =
        name.replace(Regex("\\s*\\(\\d+\\)\\s*$"), "").trim()

    private fun applyFilter() {
        val text = searchField.text.trim()
        if (text.isBlank()) {
            rebuildTree(dictionaryItems)
            return
        }

        val matchingIds = dictionaryItems
            .filter { it.parentId.isNotBlank() && it.name.contains(text, ignoreCase = true) }
            .map { it.id }
            .toSet()
        val parentIds = dictionaryItems
            .filter { it.id in matchingIds }
            .map { it.parentId }
            .toSet()
        val filtered = dictionaryItems.filter { it.id in matchingIds || it.id in parentIds }
        rebuildTree(filtered)
    }

    private fun rebuildTree(items: List<DictionaryTreeItem>) {
        val nodesById = linkedMapOf<String, DefaultMutableTreeNode>()
        clearTree()

        for (item in items) {
            nodesById[item.id] = DefaultMutableTreeNode(item)
        }

        for (item in items) {
            val node = nodesById[item.id] ?: continue

            if (item.parentId.isBlank()) {
                rootNode.add(node)
            } else {
                val parent = nodesById[item.parentId] ?: rootNode
                parent.add(node)
            }
        }

        treeModel.reload()
        expandFirstLevel()
    }

    private fun clearTree() {
        rootNode.removeAllChildren()
        treeModel.reload()
    }

    private fun expandFirstLevel() {
        for (i in 0 until rootNode.childCount) {
            tree.expandPath(tree.getPathForRow(i))
        }
    }

    private fun loadFieldsForSelection(node: DefaultMutableTreeNode) {
        val item = node.userObject as? DictionaryTreeItem ?: return
        if (item.parentId.isBlank()) return
        if (node.childCount > 0) return

        val objectType = item.toPluginObjectType() ?: findObjectTypeFromParent(node) ?: return
        val objectName = item.objectName.ifBlank { item.name }
        val cacheKey = "${objectType.apiName}|${objectName.uppercase()}"

        fieldCache[cacheKey]?.let { cachedNodes ->
            addFieldNodes(node, cachedNodes)
            return
        }

        if (!loadingFieldKeys.add(cacheKey)) return

        val loadingNode = DefaultMutableTreeNode("Cargando...")
        node.add(loadingNode)
        treeModel.reload(node)
        tree.expandPath(tree.selectionPath)

        object : SwingWorker<List<Any>, Unit>() {
            override fun doInBackground(): List<Any> {
                val fields = catalogService.loadFields(objectType.apiName, objectName)
                return buildFieldNodes(objectType, fields)
            }

            override fun done() {
                SwingUtilities.invokeLater {
                    loadingFieldKeys.remove(cacheKey)
                    node.remove(loadingNode)
                    runCatching {
                        val nodes = get()
                        fieldCache[cacheKey] = nodes
                        addFieldNodes(node, nodes)
                    }.onFailure {
                        node.add(DefaultMutableTreeNode("No se pudieron cargar campos"))
                        treeModel.reload(node)
                    }
                }
            }
        }.execute()
    }

    private fun buildFieldNodes(
        objectType: SmxDatabaseObjectType,
        databaseFields: List<SmxDatabaseField>,
    ): List<Any> {
        val parameters = databaseFields
            .filter { it.section == SmxDatabaseFieldSection.PARAMETER }
            .map(::toFieldTreeItem)
        val outputs = databaseFields
            .filter { it.section == SmxDatabaseFieldSection.OUTPUT }
            .map(::toFieldTreeItem)
        val fields = databaseFields
            .filter { it.section != SmxDatabaseFieldSection.PARAMETER && it.section != SmxDatabaseFieldSection.OUTPUT }
            .map(::toFieldTreeItem)

        if (objectType != SmxDatabaseObjectType.PROCEDURE) {
            return withNameColumnWidth(SmxFieldOrdering.sort(fields) { it.name })
        }

        val procedureFields = withNameColumnWidth(
            SmxFieldOrdering.sort(parameters) { it.name } +
                SmxFieldOrdering.sort(outputs) { it.name },
        )
        val parametersByName = procedureFields.filter { it.section == SmxDatabaseFieldSection.PARAMETER }
        val outputsByName = procedureFields.filter { it.section == SmxDatabaseFieldSection.OUTPUT }

        return buildList {
            if (parametersByName.isNotEmpty()) {
                add(SectionTreeItem("Parámetros"))
                addAll(parametersByName)
            }
            if (outputsByName.isNotEmpty()) {
                add(SectionTreeItem("Salida"))
                addAll(outputsByName)
            }
        }
    }

    private fun toFieldTreeItem(field: SmxDatabaseField): FieldTreeItem =
        FieldTreeItem(
            name = field.name,
            section = field.section,
            dataType = field.formattedType,
            nameColumnWidth = 0,
            required = field.required,
        )

    private fun withNameColumnWidth(fields: List<FieldTreeItem>): List<FieldTreeItem> {
        val maxNameWidth = fields.maxOfOrNull { getTextWidth(it.name) } ?: 0
        return fields.map { it.copy(nameColumnWidth = maxNameWidth) }
    }

    private fun getTextWidth(text: String): Int =
        tree.getFontMetrics(tree.font).stringWidth(text)

    private fun addFieldNodes(parent: DefaultMutableTreeNode, nodes: List<Any>) {
        if (nodes.isEmpty()) {
            treeModel.reload(parent)
            return
        }

        nodes.forEach { parent.add(DefaultMutableTreeNode(it)) }
        treeModel.reload(parent)
        tree.expandPath(tree.selectionPath)
    }

    private fun findObjectTypeFromParent(node: DefaultMutableTreeNode): SmxDatabaseObjectType? {
        val parent = node.parent as? DefaultMutableTreeNode ?: return null
        return (parent.userObject as? DictionaryTreeItem)
            ?.type
            ?.takeIf { it != SmxDatabaseObjectType.UNKNOWN }
    }

    private fun showContextMenu(event: MouseEvent) {
        val path = tree.getPathForLocation(event.x, event.y) ?: return
        tree.selectionPath = path

        val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
        val item = node.userObject as? DictionaryTreeItem ?: return
        if (item.parentId.isBlank()) return

        val objectType = item.toPluginObjectType() ?: findObjectTypeFromParent(node) ?: return
        if (objectType !in setOf(
                SmxDatabaseObjectType.TABLE,
                SmxDatabaseObjectType.VIEW,
                SmxDatabaseObjectType.PROCEDURE,
            )
        ) return

        val objectName = item.objectName.ifBlank { item.name }
        val group = DefaultActionGroup().apply {
            add(object : AnAction("Ver datos", "Abrir datos de $objectName", AllIcons.Actions.Preview) {
                override fun actionPerformed(e: AnActionEvent) {
                    SmxDataPreviewOpener.open(project, objectType.apiName, objectName)
                }
            })
            if (objectType == SmxDatabaseObjectType.VIEW || objectType == SmxDatabaseObjectType.PROCEDURE) {
                add(object : AnAction("Ver código", "Abrir código de $objectName", AllIcons.Actions.ShowAsTree) {
                    override fun actionPerformed(e: AnActionEvent) {
                        SmxObjectSourceOpener.open(project, objectType.apiName, objectName)
                    }
                })
            }
            add(Separator.getInstance())
            add(object : AnAction("Cargar dependencias", "Cargar dependencias de $objectName", AllIcons.Actions.Refresh) {
                override fun actionPerformed(e: AnActionEvent) {
                    loadDependencies(objectType, objectName)
                }
            })
        }

        ActionManager.getInstance()
            .createActionPopupMenu(ActionPlaces.POPUP, group)
            .component
            .show(tree, event.x, event.y)
    }

    private fun loadDependencies(objectType: SmxDatabaseObjectType, objectName: String) {
        searchField.textEditor.emptyText.text = "Cargando dependencias..."

        object : SwingWorker<List<SmxDatabaseObject>, Unit>() {
            override fun doInBackground(): List<SmxDatabaseObject> {
                val dependencies = catalogService.loadDependencies(objectType.apiName, objectName)
                return dependencyTreeObjects(dependencies)
            }

            override fun done() {
                SwingUtilities.invokeLater {
                    runCatching {
                        val objects = get()
                        val label = "Dependencias: ${objectName.uppercase()}"
                        addDependencyMode(SmxDictionaryViewMode(label, objects))
                    }.onFailure { error ->
                        searchField.textEditor.emptyText.text = error.message ?: "No se pudieron cargar dependencias"
                    }
                }
            }
        }.execute()
    }

    private fun dependencyTreeObjects(
        dependencies: List<SmxDatabaseDependency>,
    ): List<SmxDatabaseObject> {
        val dependencyTypes = dependencies.map { it.target.type }.toSet()
        val roots = allDictionaryItems
            .filter { it.parentId.isBlank() && it.type in dependencyTypes }
            .map { it.toDatabaseObject() }
        return roots + dependencies.map { it.target }
    }

    private fun resetModes() {
        updatingMode = true
        modeModel.removeAllElements()
        modeModel.addElement(SmxDictionaryViewMode.ALL)
        modeCombo.selectedIndex = 0
        modeCombo.isVisible = false
        updatingMode = false
    }

    private fun addDependencyMode(mode: SmxDictionaryViewMode) {
        updatingMode = true
        val existingIndex = findModeIndex(mode.label)
        if (existingIndex >= 0) {
            modeModel.removeElementAt(existingIndex)
        }
        modeModel.addElement(mode)
        modeCombo.selectedItem = mode
        modeCombo.isVisible = true
        updatingMode = false
        applySelectedMode()
    }

    private fun findModeIndex(label: String): Int {
        for (index in 0 until modeModel.size) {
            if (modeModel.getElementAt(index).label == label) {
                return index
            }
        }
        return -1
    }

    private fun applySelectedMode() {
        val mode = modeCombo.selectedItem as? SmxDictionaryViewMode ?: SmxDictionaryViewMode.ALL
        searchField.text = ""
        dictionaryItems = mode.objects
            ?.map(::toTreeItem)
            ?.let(::withRootTotals)
            ?: allDictionaryItems
        fieldCache.clear()
        loadingFieldKeys.clear()
        searchField.textEditor.emptyText.text = "Buscar tabla, vista o procedimiento"
        rebuildTree(dictionaryItems)
    }

    private data class DictionaryTreeItem(
        val name: String,
        val type: SmxDatabaseObjectType,
        val id: String,
        val parentId: String,
        val objectName: String,
    ) {
        override fun toString(): String = name

        fun toPluginObjectType(): SmxDatabaseObjectType? =
            type.takeIf { it != SmxDatabaseObjectType.UNKNOWN }

        fun toDatabaseObject(): SmxDatabaseObject =
            SmxDatabaseObject(
                type = type,
                name = name,
                displayName = baseDisplayName(),
                nodeId = id,
                parentId = parentId,
            )

        private fun baseDisplayName(): String =
            name.replace(Regex("\\s*\\(\\d+\\)\\s*$"), "").trim()
    }

    private data class FieldTreeItem(
        val name: String,
        val section: SmxDatabaseFieldSection,
        val dataType: String,
        val nameColumnWidth: Int,
        val required: Boolean,
    ) {
        override fun toString(): String = name
    }

    private data class SectionTreeItem(
        val name: String,
    ) {
        override fun toString(): String = name
    }

    private enum class RootNodeType {
        TABLES,
        VIEWS,
        PROCEDURES,
    }

    private inner class FieldSelectionListener : TreeSelectionListener {
        override fun valueChanged(e: TreeSelectionEvent) {
            val node = e.path.lastPathComponent as? DefaultMutableTreeNode ?: return
            loadFieldsForSelection(node)
        }
    }

    private inner class DictionaryPopupListener : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
            if (e.isPopupTrigger) showContextMenu(e)
        }

        override fun mouseReleased(e: MouseEvent) {
            if (e.isPopupTrigger) showContextMenu(e)
        }
    }

    private class TransparentTreeCellRenderer : DefaultTreeCellRenderer() {
        private val parameterColor = JBColor(0x8E44AD, 0xC792EA)
        private val outputColor = JBColor(0x0B6E99, 0x82AAFF)
        private val relationColor = JBColor(0xB05A00, 0xF0A45D)
        private val idColor = JBColor(0x2E7D32, 0x89D185)
        private val tableColor = JBColor(0x1565C0, 0x82AAFF)
        private val viewColor = JBColor(0x00897B, 0x80CBC4)
        private val procedureColor = JBColor(0x6A1B9A, 0xC792EA)
        private val gapWidth = JBUI.scale(18)

        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean,
        ): Component {
            val component = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
            isOpaque = selected
            backgroundNonSelectionColor = null
            background = if (selected) backgroundSelectionColor else null
            font = font.deriveFont(Font.PLAIN)

            val textValue = value
                ?.let { it as? DefaultMutableTreeNode }
                ?.userObject
                ?.toString()
                .orEmpty()

            val node = value as? DefaultMutableTreeNode
            val rootType = node?.rootNodeType()

            when {
                rootType == RootNodeType.TABLES -> icon = AllIcons.Nodes.DataTables
                rootType == RootNodeType.VIEWS -> icon = AllIcons.Actions.Preview
                rootType == RootNodeType.PROCEDURES -> icon = AllIcons.Nodes.Function
                node?.isObjectUnder(RootNodeType.TABLES) == true -> icon = AllIcons.Nodes.DataTables
                node?.isObjectUnder(RootNodeType.VIEWS) == true -> icon = AllIcons.Actions.Preview
                node?.isObjectUnder(RootNodeType.PROCEDURES) == true -> icon = AllIcons.Nodes.Function
            }

            if (!selected) {
                when {
                    rootType == RootNodeType.TABLES -> {
                        foreground = tableColor
                        font = font.deriveFont(Font.BOLD)
                    }
                    rootType == RootNodeType.VIEWS -> {
                        foreground = viewColor
                        font = font.deriveFont(Font.BOLD)
                    }
                    rootType == RootNodeType.PROCEDURES -> {
                        foreground = procedureColor
                        font = font.deriveFont(Font.BOLD)
                    }
                    textValue == "Parámetros" -> {
                        foreground = parameterColor
                        font = font.deriveFont(Font.BOLD)
                    }
                    textValue == "Salida" -> {
                        foreground = outputColor
                        font = font.deriveFont(Font.BOLD)
                    }
                    else -> {
                        val fieldName = textValue.uppercase()
                        when {
                            fieldName.startsWith("RELA_") -> foreground = relationColor
                            fieldName.startsWith("ID_") -> {
                                foreground = idColor
                                font = font.deriveFont(Font.BOLD)
                            }
                            fieldName.endsWith("_FAPL") -> {
                                foreground = idColor
                                font = font.deriveFont(Font.BOLD)
                            }
                        }
                    }
                }
            }

            val fieldItem = (value as? DefaultMutableTreeNode)?.userObject as? FieldTreeItem
            if (fieldItem != null) {
                return buildFieldComponent(fieldItem, selected, hasFocus, leaf)
            }

            return component
        }

        private fun buildFieldComponent(
            fieldItem: FieldTreeItem,
            selected: Boolean,
            hasFocus: Boolean,
            leaf: Boolean,
        ): Component {
            val panel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                isOpaque = selected
                border = null
            }
            val nameLabel = JLabel(fieldItem.name).apply {
                icon = leafIcon
                font = this@TransparentTreeCellRenderer.font.deriveFont(Font.PLAIN)
                foreground = this@TransparentTreeCellRenderer.foregroundForField(fieldItem.name)
                preferredSize = Dimension(fieldItem.nameColumnWidth + icon.iconWidth + iconTextGap, preferredSize.height)
            }
            val typeLabel = JLabel(fieldItem.dataType).apply {
                font = nameLabel.font
                foreground = nameLabel.foreground
            }
            val requiredLabel = JLabel(if (fieldItem.required) "✔" else "").apply {
                font = nameLabel.font.deriveFont(Font.BOLD)
                foreground = nameLabel.foreground
                horizontalAlignment = JLabel.LEFT
                preferredSize = Dimension(JBUI.scale(20), preferredSize.height)
                minimumSize = preferredSize
            }

            if (fieldItem.name.uppercase().startsWith("ID_") || fieldItem.name.uppercase().endsWith("_FAPL")) {
                nameLabel.font = nameLabel.font.deriveFont(Font.BOLD)
                typeLabel.font = typeLabel.font.deriveFont(Font.BOLD)
                requiredLabel.font = requiredLabel.font.deriveFont(Font.BOLD)
            }

            if (selected) {
                panel.background = backgroundSelectionColor
                nameLabel.foreground = textSelectionColor
                typeLabel.foreground = textSelectionColor
                requiredLabel.foreground = textSelectionColor
            }

            panel.add(nameLabel)
            panel.add(Box.createHorizontalStrut(gapWidth))
            panel.add(typeLabel)
            panel.add(Box.createHorizontalStrut(JBUI.scale(10)))
            panel.add(requiredLabel)
            return panel
        }

        private fun foregroundForField(fieldName: String): java.awt.Color {
            val normalized = fieldName.uppercase()
            return when {
                normalized.startsWith("RELA_") -> relationColor
                normalized.startsWith("ID_") || normalized.endsWith("_FAPL") -> idColor
                else -> textNonSelectionColor
            }
        }

        private fun DefaultMutableTreeNode.rootNodeType(): RootNodeType? {
            val item = userObject as? DictionaryTreeItem ?: return null
            if (item.parentId.isNotBlank()) return null
            return when (item.type) {
                SmxDatabaseObjectType.TABLE -> RootNodeType.TABLES
                SmxDatabaseObjectType.VIEW -> RootNodeType.VIEWS
                SmxDatabaseObjectType.PROCEDURE -> RootNodeType.PROCEDURES
                SmxDatabaseObjectType.UNKNOWN -> null
            }
        }

        private fun DefaultMutableTreeNode.isObjectUnder(rootType: RootNodeType): Boolean {
            val item = userObject as? DictionaryTreeItem ?: return false
            if (item.parentId.isBlank()) return false
            val parent = parent as? DefaultMutableTreeNode ?: return false
            return parent.rootNodeType() == rootType
        }

    }
}
