package mx.jars.venture.dataPreview.layout

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JCheckBox

class SmxDataPreviewToolbar(
    private val onFirst: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit,
    private val onLast: () -> Unit,
    private val onHideRelaChanged: (Boolean) -> Unit,
    private val onApplyFilters: () -> Unit,
    private val onClearFilters: () -> Unit,
) : JBPanel<SmxDataPreviewToolbar>(FlowLayout(FlowLayout.LEFT)) {
    private val firstButton = JButton("<<")
    private val previousButton = JButton("<")
    private val nextButton = JButton(">")
    private val lastButton = JButton(">>")
    private val statusLabel = JBLabel()
    private val hideRelaCheckBox = JCheckBox("Ocultar RELA")
    private val applyFiltersButton = JButton("Aplicar filtros")
    private val clearFiltersButton = JButton("Limpiar filtros")

    init {
        add(firstButton)
        add(previousButton)
        add(statusLabel)
        add(nextButton)
        add(lastButton)
        add(hideRelaCheckBox)
        add(applyFiltersButton)
        add(clearFiltersButton)

        firstButton.addActionListener { onFirst() }
        previousButton.addActionListener { onPrevious() }
        nextButton.addActionListener { onNext() }
        lastButton.addActionListener { onLast() }
        hideRelaCheckBox.addActionListener { onHideRelaChanged(hideRelaCheckBox.isSelected) }
        applyFiltersButton.addActionListener { onApplyFilters() }
        clearFiltersButton.addActionListener { onClearFilters() }
    }

    fun setStatus(text: String) {
        statusLabel.text = text
    }

    fun hideRela(): Boolean = hideRelaCheckBox.isSelected

    fun setBusy(loading: Boolean, activePage: Int, totalPages: Int) {
        firstButton.isEnabled = !loading
        previousButton.isEnabled = !loading && activePage > 1
        nextButton.isEnabled = !loading && (totalPages == 0 || activePage < totalPages)
        lastButton.isEnabled = !loading && (totalPages == 0 || activePage < totalPages)
        hideRelaCheckBox.isEnabled = !loading
        applyFiltersButton.isEnabled = !loading
        clearFiltersButton.isEnabled = !loading
        if (loading) {
            setStatus("Cargando...")
        }
    }
}
