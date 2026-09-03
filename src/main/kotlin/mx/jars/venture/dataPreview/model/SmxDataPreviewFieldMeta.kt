package mx.jars.venture.dataPreview.model

data class SmxDataPreviewFieldMeta(
    val name: String,
    val dataType: String,
    val section: String = "",
    val length: Int = 0,
) {
    private val normalizedType = dataType.uppercase().trim()

    fun isDateOnly(): Boolean = normalizedType == "DATE"

    fun isTimestamp(): Boolean = normalizedType == "TIMESTAMP"

    fun isNumeric(): Boolean =
        normalizedType in NUMERIC_TYPES ||
            normalizedType.startsWith("NUMERIC") ||
            normalizedType.startsWith("DECIMAL")

    fun isParameter(): Boolean = section.uppercase() == "PARAMETRO"

    fun formattedType(): String {
        if (!normalizedType.equals("VARCHAR", ignoreCase = true) || length <= 0) {
            return dataType
        }
        return "VARCHAR($length)"
    }

    companion object {
        private val NUMERIC_TYPES = setOf(
            "SMALLINT",
            "INTEGER",
            "INT",
            "BIGINT",
            "FLOAT",
            "DOUBLE PRECISION",
            "DOUBLE",
        )
    }
}
