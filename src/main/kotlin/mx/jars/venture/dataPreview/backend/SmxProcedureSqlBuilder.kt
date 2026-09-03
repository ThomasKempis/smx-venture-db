package mx.jars.venture.dataPreview.backend

import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta

class SmxProcedureSqlBuilder(
    private val sqlBuilder: SmxDataPreviewSqlBuilder = SmxDataPreviewSqlBuilder(),
) {
    fun buildSelect(
        procedureName: String,
        values: Map<String, String>,
        fieldMeta: Map<String, SmxDataPreviewFieldMeta>,
    ): String {
        val safeProcedureName = sqlBuilder.safeSqlObjectName(procedureName)
        val parameters = fieldMeta.values
            .filter { it.isParameter() }
            .joinToString(", ") { parameter ->
                valueToSql(parameter, values[parameter.name].orEmpty())
            }

        return if (parameters.isBlank()) {
            "SELECT * FROM $safeProcedureName"
        } else {
            "SELECT * FROM $safeProcedureName($parameters)"
        }
    }

    private fun valueToSql(parameter: SmxDataPreviewFieldMeta, value: String): String {
        if (value.isBlank()) return "NULL"
        if (parameter.isNumeric()) return value.replace(",", "")
        return "'${sql(value)}'"
    }

    private fun sql(value: String): String =
        value.trim().replace("'", "''")
}
