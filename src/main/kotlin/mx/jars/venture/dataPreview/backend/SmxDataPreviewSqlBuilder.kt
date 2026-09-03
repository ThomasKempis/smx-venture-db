package mx.jars.venture.dataPreview.backend

import mx.jars.venture.dataPreview.model.SmxDataPreviewFilter
import mx.jars.venture.dataPreview.model.SmxDataPreviewSortDirection

class SmxDataPreviewSqlBuilder {
    fun buildSelect(objectName: String): String =
        "SELECT * FROM ${safeSqlObjectName(objectName)}"

    fun buildOrder(orderField: String?, orderDirection: SmxDataPreviewSortDirection): String =
        orderField
            ?.takeIf { it.isNotBlank() }
            ?.let { " ORDER BY ${safeSqlObjectName(it)} ${orderDirection.sql}" }
            .orEmpty()

    fun buildWhere(filters: List<SmxDataPreviewFilter>): String {
        val conditions = filters
            .filter { it.value.isNotBlank() }
            .map {
                val fieldName = safeSqlObjectName(it.fieldName)
                val value = sqlString("%${it.value.trim()}%")
                "UPPER(CAST($fieldName AS VARCHAR(500))) LIKE UPPER('$value')"
            }

        return if (conditions.isEmpty()) "" else " WHERE ${conditions.joinToString(" AND ")}"
    }

    fun buildPage(sqlBase: String, sqlOrder: String, pageSize: Int, page: Int): String {
        val skip = ((page - 1).coerceAtLeast(0)) * pageSize
        return if (skip <= 0) {
            "SELECT FIRST $pageSize ${formatSqlBase(sqlBase)}$sqlOrder"
        } else {
            "SELECT FIRST $pageSize SKIP $skip ${formatSqlBase(sqlBase)}$sqlOrder"
        }
    }

    fun safeSqlObjectName(objectName: String): String {
        val cleanName = objectName.trim().uppercase()
        require(cleanName.matches(Regex("[A-Z0-9_$]+"))) {
            "Nombre de objeto no válido"
        }
        return cleanName
    }

    private fun formatSqlBase(sqlString: String): String =
        sqlString.trim().replace(Regex("^SELECT\\s+", RegexOption.IGNORE_CASE), "")

    private fun sqlString(value: String): String =
        value.replace("'", "''")
}
