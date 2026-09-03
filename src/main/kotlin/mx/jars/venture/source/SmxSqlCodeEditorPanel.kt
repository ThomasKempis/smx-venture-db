package mx.jars.venture.source

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

class SmxSqlCodeEditorPanel(
    initialText: String,
) : JPanel(BorderLayout()) {
    private val originalText = initialText
    private val textPane = object : JTextPane() {
        override fun getScrollableTracksViewportWidth(): Boolean = false
    }.apply {
        font = Font(Font.MONOSPACED, Font.PLAIN, 13)
        margin = JBUI.insets(8)
        isOpaque = true
        document = javax.swing.text.DefaultStyledDocument()
    }
    private val scrollPane = JBScrollPane(textPane)
    private val lineNumbers = LineNumberView(textPane)
    private var highlightScheduled = false
    private val highlightListener = object : DocumentListener {
        override fun insertUpdate(event: DocumentEvent) = scheduleHighlight()
        override fun removeUpdate(event: DocumentEvent) = scheduleHighlight()
        override fun changedUpdate(event: DocumentEvent) = Unit
    }

    init {
        textPane.text = initialText
        scrollPane.setRowHeaderView(lineNumbers)
        scrollPane.horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        scrollPane.border = JBUI.Borders.empty()
        add(scrollPane, BorderLayout.CENTER)
        SwingUtilities.invokeLater {
            scrollPane.horizontalScrollBar.value = 0
            scrollPane.verticalScrollBar.value = 0
            scrollPane.viewport.viewPosition = Point(0, 0)
        }

        textPane.document.addDocumentListener(highlightListener)
        textPane.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent) {
                lineNumbers.revalidate()
                lineNumbers.repaint()
            }
        })
        highlight()
    }

    fun getText(): String = textPane.text

    fun getEditorComponent(): JComponent = scrollPane

    fun getPreferredFocusedComponent(): JComponent = textPane

    fun isModified(): Boolean = getText() != originalText

    fun dispose() {
        textPane.document.removeDocumentListener(highlightListener)
    }

    private fun scheduleHighlight() {
        if (!highlightScheduled) {
            highlightScheduled = true
            SwingUtilities.invokeLater {
                highlightScheduled = false
                highlight()
            }
        }
        lineNumbers.repaint()
    }

    private fun highlight() {
        val document = textPane.document as StyledDocument
        val caretPosition = textPane.caretPosition
        val selectionStart = textPane.selectionStart
        val selectionEnd = textPane.selectionEnd
        val text = document.getText(0, document.length)
        document.setCharacterAttributes(0, document.length, baseAttributes, true)

        val tokens = tokenize(text)
        val tableReferences = findTableReferences(tokens)
        val references = tableReferences.lookup
        val referenceAttributes = references.values.distinct().associateWith(::referenceAttributes)

        tokens.forEachIndexed { index, token ->
            val attributes = when {
                token.kind == TokenKind.COMMENT -> commentAttributes
                token.kind == TokenKind.STRING -> stringAttributes
                token.kind == TokenKind.WORD -> {
                    val word = token.text.uppercase()
                    when {
                        word in SQL_KEYWORDS -> keywordAttributes
                        else -> null
                    }
                }
                token.kind == TokenKind.SYMBOL && token.text in SQL_KEYWORDS -> keywordAttributes
                else -> null
            }
            if (attributes != null) {
                document.setCharacterAttributes(token.start, token.text.length, attributes, false)
            }

            if (token.kind == TokenKind.WORD && index + 2 < tokens.size &&
                tokens[index + 1].text == "." &&
                tokens[index + 2].kind == TokenKind.WORD
            ) {
                references[token.text.uppercase()]?.let { reference ->
                    document.setCharacterAttributes(
                        token.start,
                        token.text.length,
                        referenceAttributes.getValue(reference),
                        false,
                    )
                }
            }
        }

        tableReferences.declarations.forEach { (tokenIndex, reference) ->
            val token = tokens[tokenIndex]
            if (token.kind == TokenKind.WORD) {
                document.setCharacterAttributes(
                    token.start,
                    token.text.length,
                    referenceAttributes.getValue(reference),
                    false,
                )
            }
        }

        tokens.filter { it.kind == TokenKind.WORD }.let { wordTokens ->
            var previousWord: String? = null
            wordTokens.forEach { token ->
                if (previousWord in OBJECT_CONTEXT_KEYWORDS && token.text.uppercase() !in SQL_KEYWORDS &&
                    references[token.text.uppercase()] == null
                ) {
                    document.setCharacterAttributes(token.start, token.text.length, objectAttributes, false)
                }
                if (token.text.uppercase() in SQL_KEYWORDS) {
                    previousWord = token.text.uppercase()
                } else {
                    previousWord = null
                }
            }
        }
        textPane.caretPosition = caretPosition.coerceAtMost(document.length)
        if (selectionStart != selectionEnd) {
            textPane.select(
                selectionStart.coerceAtMost(document.length),
                selectionEnd.coerceAtMost(document.length),
            )
        }
    }

    private fun tokenize(text: String): List<SqlToken> =
        TOKEN_PATTERN.matcher(text).let { matcher ->
            buildList {
                while (matcher.find()) {
                    val token = matcher.group()
                    add(
                        SqlToken(
                            matcher.start(),
                            token,
                            when {
                                token.startsWith("--") -> TokenKind.COMMENT
                                token.startsWith("'") -> TokenKind.STRING
                                token.matches(WORD_PATTERN) -> TokenKind.WORD
                                else -> TokenKind.SYMBOL
                            },
                        ),
                    )
                }
            }
        }

    private fun findTableReferences(tokens: List<SqlToken>): TableReferences {
        val references = linkedMapOf<String, String>()
        val declarations = linkedMapOf<Int, String>()
        var index = 0

        while (index < tokens.size) {
            val token = tokens[index]
            if (token.kind != TokenKind.WORD || token.text.uppercase() !in RELATION_KEYWORDS) {
                index++
                continue
            }

            val firstRelationIndex = nextMeaningfulToken(tokens, index + 1)
            val relationStart = firstRelationIndex ?: run {
                index++
                continue
            }
            if (tokens[relationStart].text == "(") {
                index++
                continue
            }
            var relationIndex = relationStart

            val relationParts = mutableListOf<String>()
            val relationTokenIndexes = mutableListOf<Int>()
            while (relationIndex < tokens.size && tokens[relationIndex].kind == TokenKind.WORD) {
                relationParts += tokens[relationIndex].text
                relationTokenIndexes += relationIndex
                val dotIndex = relationIndex + 1
                val nextIndex = nextMeaningfulToken(tokens, dotIndex)
                if (nextIndex == null || tokens[nextIndex].text != ".") break
                val followingIndex = nextMeaningfulToken(tokens, nextIndex + 1) ?: break
                relationIndex = followingIndex
            }

            if (relationParts.isEmpty()) {
                index++
                continue
            }

            val relationName = relationParts.joinToString(".").uppercase()
            val reference = "REFERENCE_$relationName"
            references[relationName] = reference
            references[relationParts.last().uppercase()] = reference
            relationTokenIndexes.forEach { declarations[it] = reference }

            val aliasIndex = nextMeaningfulToken(tokens, (relationTokenIndexes.lastOrNull() ?: index) + 1)
            if (aliasIndex != null && tokens[aliasIndex].kind == TokenKind.WORD) {
                val alias = tokens[aliasIndex].text.uppercase()
                if (alias == "AS") {
                    val explicitAlias = nextMeaningfulToken(tokens, aliasIndex + 1)
                    if (explicitAlias != null && tokens[explicitAlias].kind == TokenKind.WORD) {
                        references[tokens[explicitAlias].text.uppercase()] = reference
                        declarations[explicitAlias] = reference
                    }
                } else if (alias !in SQL_KEYWORDS) {
                    references[alias] = reference
                    declarations[aliasIndex] = reference
                }
            }
            index = relationTokenIndexes.lastOrNull()?.plus(1) ?: index + 1
        }
        return TableReferences(references, declarations)
    }

    private fun nextMeaningfulToken(tokens: List<SqlToken>, start: Int): Int? =
        (start until tokens.size).firstOrNull {
            tokens[it].kind != TokenKind.COMMENT && tokens[it].kind != TokenKind.STRING
        }

    private fun referenceAttributes(reference: String): SimpleAttributeSet =
        SimpleAttributeSet().apply {
            val colorIndex = (reference.hashCode() and Int.MAX_VALUE) % referenceColors.size
            val color = referenceColors[colorIndex]
            StyleConstants.setForeground(this, color)
            StyleConstants.setBold(this, true)
        }

    private data class SqlToken(
        val start: Int,
        val text: String,
        val kind: TokenKind,
    )

    private data class TableReferences(
        val lookup: Map<String, String>,
        val declarations: Map<Int, String>,
    )

    private enum class TokenKind {
        WORD,
        STRING,
        COMMENT,
        SYMBOL,
    }

    private class LineNumberView(
        private val textPane: JTextPane,
    ) : JComponent() {
        private val numberColor = JBColor(Color(0x7A7A7A), Color(0x8C8C8C))
        private val backgroundColor = JBColor(Color(0xF5F5F5), Color(0x2B2B2B))

        init {
            background = backgroundColor
            border = JBUI.Borders.emptyRight(8)
            textPane.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(event: DocumentEvent) = repaint()
                override fun removeUpdate(event: DocumentEvent) = repaint()
                override fun changedUpdate(event: DocumentEvent) = repaint()
            })
        }

        override fun getPreferredSize(): java.awt.Dimension =
            java.awt.Dimension(numberWidth(), textPane.height)

        override fun paintComponent(graphics: Graphics) {
            super.paintComponent(graphics)
            val graphics2d = graphics.create()
            graphics2d.color = numberColor
            graphics2d.font = textPane.font

            val visible = textPane.visibleRect
            val start = textPane.viewToModel2D(java.awt.Point(0, visible.y))
            val end = textPane.viewToModel2D(java.awt.Point(0, visible.y + visible.height))
            val root = textPane.document.defaultRootElement
            var line = root.getElementIndex(start)
            val lastLine = root.getElementIndex(end).coerceAtMost(root.elementCount - 1)

            while (line <= lastLine) {
                val element = root.getElement(line)
                try {
                    val rectangle = textPane.modelToView2D(element.startOffset).bounds
                    graphics2d.drawString((line + 1).toString(), 4, rectangle.y + getFontMetrics(textPane.font).ascent)
                } catch (_: BadLocationException) {
                    break
                }
                line++
            }
            graphics2d.dispose()
        }

        private fun numberWidth(): Int {
            val digits = textPane.document.defaultRootElement.elementCount.toString().length
            return 12 + getFontMetrics(textPane.font).charWidth('0') * digits
        }
    }

    companion object {
        private val baseAttributes = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, JBColor.namedColor("Editor.foreground", JBColor.BLACK))
        }
        private val keywordAttributes = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, JBColor(0x0033B3, 0x569CD6))
            StyleConstants.setBold(this, true)
        }
        private val objectAttributes = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, JBColor(0x7A3E9D, 0xDCDCAA))
        }
        private val stringAttributes = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, JBColor(0x067D17, 0xCE9178))
        }
        private val commentAttributes = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, JBColor(0x808080, 0x6A9955))
        }
        private val SQL_KEYWORDS = setOf(
            "AS", "AND", "ASC", "BEGIN", "BY", "CASE", "CAST", "COMMIT", "CREATE",
            "DELETE", "DESC", "DISTINCT", "ELSE", "END", "EXECUTE", "EXISTS", "FROM",
            "FUNCTION", "GROUP", "HAVING", "IN", "INSERT", "INTO", "IS", "JOIN",
            "INNER", "LEFT", "RIGHT", "EXTRACT", "IIF", "COALESCE", "TRIM", "FIRST",
            "LIKE", "NOT", "NULL", "ON", "OR", "ORDER", "PROCEDURE", "SELECT",
            "SET", "THEN", "UNION", "UPDATE", "VALUES", "VIEW", "WHEN", "WHERE",
            "WITH", "IF",  "SUSPEND", "DECLARE", "VARIABLE", "BIGINT", "VARCHAR", "INTEGER",
            "DOUBLE", "PRECISION", "(", ")", "CURRENT_USER", "CURRENT_TIMESTAMP"
        )
        private val OBJECT_CONTEXT_KEYWORDS = setOf("FROM", "JOIN", "INTO", "UPDATE", "TABLE", "VIEW")
        private val RELATION_KEYWORDS = setOf("FROM", "JOIN", "INTO", "UPDATE")
        private val WORD_PATTERN = Regex("[A-Za-z_][A-Za-z0-9_$]*")
        private val TOKEN_PATTERN = Pattern.compile("--[^\\r\\n]*|'(?:''|[^'])*'|[A-Za-z_][A-Za-z0-9_$]*|\\S")
        private val referenceColors = listOf(
            JBColor(0x1565C0, 0x82AAFF),
            JBColor(0x8E44AD, 0xD7A8FF),
            JBColor(0x00897B, 0x63D8C8),
            JBColor(0xC05A00, 0xFFB46E),
            JBColor(0xAD1457, 0xFF8FC4),
            JBColor(0x558B2F, 0xB6E27A),
        )
    }
}
