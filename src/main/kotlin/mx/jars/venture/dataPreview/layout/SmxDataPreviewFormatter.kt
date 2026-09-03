package mx.jars.venture.dataPreview.layout

import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonField
import java.time.format.DateTimeFormatter
import java.util.Locale

class SmxDataPreviewFormatter {
    fun formatCell(column: String, field: SmxJsonField?, fieldMeta: Map<String, SmxDataPreviewFieldMeta>): String {
        if (field == null) return ""
        val date = field.valDate ?: return field.toString()
        val meta = fieldMeta[column.uppercase()]
        return if (meta?.isDateOnly() == true || (meta == null && date.hour == 0 && date.minute == 0 && date.second == 0)) {
            date.format(DATE_FORMAT)
        } else {
            date.format(TIMESTAMP_FORMAT)
        }
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM y", Locale.forLanguageTag("es"))
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("d MMM y H:mm", Locale.forLanguageTag("es"))
    }
}
