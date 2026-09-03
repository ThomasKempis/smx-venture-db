package mx.jars.venture.dataPreview.layout

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class SmxProcedureParameterPanel(
    private val onExecute: () -> Unit,
) : JBPanel<SmxProcedureParameterPanel>(BorderLayout()) {
    private val toggleButton = JButton("Parámetros ▾")
    private val body = JBPanel<JBPanel<*>>(GridBagLayout())
    private val inputs = linkedMapOf<String, JBTextField>()
    private var expanded = true

    init {
        isVisible = false
        border = JBUI.Borders.empty(6, 0, 8, 0)
        add(
            JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                add(toggleButton)
            },
            BorderLayout.NORTH,
        )
        add(body, BorderLayout.CENTER)

        toggleButton.addActionListener {
            setExpanded(!expanded)
        }
    }

    fun setParameters(parameters: List<SmxDataPreviewFieldMeta>) {
        inputs.clear()
        body.removeAll()
        isVisible = parameters.isNotEmpty()

        parameters.forEachIndexed { index, parameter ->
            val constraints = GridBagConstraints().apply {
                gridx = 0
                gridy = index
                anchor = GridBagConstraints.WEST
                insets = JBUI.insets(2, 0, 2, 8)
            }
            body.add(
                JLabel("${parameter.name}  ${parameter.formattedType()}").apply {
                    foreground = parameterColor(parameter.name)
                },
                constraints,
            )

            val input = JBTextField().apply {
                preferredSize = Dimension(JBUI.scale(240), preferredSize.height)
            }
            inputs[parameter.name] = input
            body.add(
                input,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = index
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = JBUI.insets(2, 0, 2, 0)
                },
            )
        }

        body.add(
            JButton("Ejecutar").apply { addActionListener { onExecute() } },
            GridBagConstraints().apply {
                gridx = 1
                gridy = parameters.size
                anchor = GridBagConstraints.EAST
                insets = JBUI.insets(6, 0, 0, 0)
            },
        )

        setExpanded(true)
        revalidate()
        repaint()
    }

    fun values(): Map<String, String> =
        inputs.mapValues { it.value.text.trim() }

    fun collapse() {
        setExpanded(false)
    }

    fun setBusy(busy: Boolean) {
        toggleButton.isEnabled = !busy
        setInputsEnabled(this, !busy)
    }

    private fun setExpanded(value: Boolean) {
        expanded = value
        body.isVisible = expanded
        toggleButton.text = if (expanded) "Parámetros ▾" else "Parámetros ▸"
        revalidate()
        repaint()
    }

    private fun setInputsEnabled(panel: JPanel, enabled: Boolean) {
        panel.components.forEach { component ->
            component.isEnabled = enabled
            if (component is JPanel) {
                setInputsEnabled(component, enabled)
            }
        }
    }

    private fun parameterColor(name: String): Color {
        val normalized = name.uppercase()
        return when {
            normalized.startsWith("RELA_") -> JBColor(0xB05A00, 0xF0A45D)
            normalized.startsWith("ID_") || normalized.endsWith("_FAPL") -> JBColor(0x2E7D32, 0x89D185)
            else -> JBColor.foreground()
        }
    }
}
