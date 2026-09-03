package mx.jars.venture

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.AsyncProcessIcon
import mx.jars.venture.connection.SmxConnectionClient
import mx.jars.venture.connection.SmxSessionService
import mx.jars.venture.ui.SmxDictionaryTreePanel
import mx.jars.venture.venture_core_connect.SmxConstantsCore
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JPasswordField
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.SwingWorker

class MyToolWindowFactory : ToolWindowFactory {
    override fun shouldBeAvailable(project: Project) = true

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(project)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
        toolWindow.setTitleActions(listOf(ChangeConnectionAction(myToolWindow)))
    }

    private class ChangeConnectionAction(
        private val myToolWindow: MyToolWindow,
    ) : AnAction("Cambiar conexión", "Abrir formulario de conexión", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
            myToolWindow.showLogon()
        }
    }

    class MyToolWindow(
        project: Project,
    ) {
        private val usuarioLabel = JBLabel("Usuario")
        private val claveLabel = JBLabel("Clave")
        private val instanciaLabel = JBLabel("Instancia")
        private val sessionProfile = SmxSessionService.currentProfile()
        private val usuarioField = JTextField(sessionProfile.usuario.ifBlank { SmxConstantsCore.usuario }, 18)
        private val claveField = JPasswordField(sessionProfile.clave.ifBlank { SmxConstantsCore.clave }, 18)
        private val instanciaField = JTextField(sessionProfile.instancia.ifBlank { "VENTURE" }, 18)
        private val statusLabel = JBLabel("Sin conectar")
        private val statusPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT, 0, 0))
        private val connectedColor = Color(0x2E7D32)
        private val conectarButton = JButton("Conectar")
        private val progressIcon = AsyncProcessIcon("Conectando").apply {
            isVisible = false
        }
        private val dictionaryTreePanel = SmxDictionaryTreePanel(project)

        private val content = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(buildForm(), BorderLayout.NORTH)
            add(dictionaryTreePanel, BorderLayout.CENTER)
        }

        fun getContent(): JBPanel<JBPanel<*>> = content

        private fun buildForm(): JBPanel<JBPanel<*>> {
            val panel = JBPanel<JBPanel<*>>(GridBagLayout())
            val c = GridBagConstraints().apply {
                insets = Insets(6, 8, 6, 8)
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
            }

            addRow(panel, c, 0, usuarioLabel, usuarioField)
            addRow(panel, c, 1, claveLabel, claveField)
            addRow(panel, c, 2, instanciaLabel, instanciaField)

            c.gridx = 1
            c.gridy = 3
            c.weightx = 1.0
            panel.add(conectarButton, c)

            c.gridy = 4
            panel.add(progressIcon, c)

            statusPanel.add(statusLabel)

            c.gridy = 5
            panel.add(statusPanel, c)

            conectarButton.addActionListener {
                callLogon()
            }

            return panel
        }

        private fun addRow(panel: JBPanel<JBPanel<*>>, c: GridBagConstraints, row: Int, label: JBLabel, field: JTextField) {
            c.gridx = 0
            c.gridy = row
            c.weightx = 0.0
            panel.add(label, c)

            c.gridx = 1
            c.weightx = 1.0
            panel.add(field, c)
        }

        private fun callLogon() {
            val usuario = usuarioField.text.trim()
            val clave = String(claveField.password)
            val instancia = instanciaField.text.trim()

            setLoading(true)
            setStatus("Conectando...")

            object : SwingWorker<Boolean, Unit>() {
                private val client = SmxConnectionClient()
                private var dictionary: SmxRowSet? = null
                private var message: String = ""

                override fun doInBackground(): Boolean {
                    return runCatching {
                        dictionary = client.connectAndLoadDictionary(usuario, clave, instancia, produccion = true)
                        val profile = SmxSessionService.currentProfile()
                        message = "● Conectado a ${profile.instancia} como ${profile.usuario}"
                        true
                    }.getOrElse { error ->
                        message = SmxSessionService.currentError() ?: error.message ?: "Usuario no reconocido"
                        SmxSessionService.recordError(message)
                        false
                    }
                }

                override fun done() {
                    SwingUtilities.invokeLater {
                        val connected = get()
                        setLoading(false)
                        setConnectedUi(connected)
                        setStatus(message, connected)
                        if (connected) {
                            dictionaryTreePanel.showDictionary(dictionary ?: SmxRowSet.empty())
                        } else {
                            dictionaryTreePanel.clear()
                        }
                    }
                }
            }.execute()
        }

        private fun setLoading(loading: Boolean) {
            usuarioField.isEnabled = !loading
            claveField.isEnabled = !loading
            instanciaField.isEnabled = !loading
            conectarButton.isEnabled = !loading
            progressIcon.isVisible = loading
        }

        fun showLogon() {
            SmxSessionService.markDisconnected()
            setConnectedUi(false)
            setStatus("Sin conectar")
            dictionaryTreePanel.clear()
        }

        private fun setConnectedUi(connected: Boolean) {
            usuarioLabel.isVisible = !connected
            usuarioField.isVisible = !connected
            claveLabel.isVisible = !connected
            claveField.isVisible = !connected
            instanciaLabel.isVisible = !connected
            instanciaField.isVisible = !connected
            conectarButton.isVisible = !connected
            progressIcon.isVisible = false
            content.revalidate()
            content.repaint()
        }

        private fun setStatus(message: String, connected: Boolean = false) {
            statusLabel.foreground = if (connected) connectedColor else null
            statusLabel.text = message
        }
    }
}
