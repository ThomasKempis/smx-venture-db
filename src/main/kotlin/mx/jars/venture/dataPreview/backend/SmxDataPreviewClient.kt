package mx.jars.venture.dataPreview.backend

import mx.jars.venture.connection.SmxSessionService
import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta
import mx.jars.venture.dataPreview.model.SmxDataPreviewFilter
import mx.jars.venture.dataPreview.model.SmxDataPreviewSortDirection
import mx.jars.venture.dataPreview.model.SmxDictionaryObject
import mx.jars.venture.venture_core_connect.SmxConstantsCore
import mx.jars.venture.venture_core_connect.smxDataBase.SmxDb
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonCall

class SmxDataPreviewClient {
    private val sqlBuilder = SmxDataPreviewSqlBuilder()
    private val procedureSqlBuilder = SmxProcedureSqlBuilder(sqlBuilder)

    fun loadPage(
        objectType: String,
        objectName: String,
        orderField: String?,
        orderDirection: SmxDataPreviewSortDirection,
        pageSize: Int,
        page: Int,
        procedureParameters: Map<String, String> = emptyMap(),
        fieldMeta: Map<String, SmxDataPreviewFieldMeta> = emptyMap(),
        filters: List<SmxDataPreviewFilter> = emptyList(),
    ): SmxRowSet {
        val smxDb = buildDb()
        val skip = ((page - 1).coerceAtLeast(0)) * pageSize
        val sqlBase = if (objectType.uppercase() == "PROCEDURE") {
            procedureSqlBuilder.buildSelect(objectName, procedureParameters, fieldMeta)
        } else {
            sqlBuilder.buildSelect(objectName)
        }
        val sqlWhere = sqlBuilder.buildWhere(filters)
        val sqlOrder = sqlBuilder.buildOrder(orderField, orderDirection)
        val sqlFilteredBase = "$sqlBase$sqlWhere"

        if (skip <= 0) {
            if (objectType.uppercase() == "PROCEDURE") {
                smxDb.addSql(TMPDATA01, sqlBuilder.buildPage(sqlFilteredBase, sqlOrder, pageSize, page))
            } else {
                smxDb.addSqlParcing(TMPDATA01, "$sqlFilteredBase$sqlOrder")
            }
        } else {
            smxDb.addSql(
                TMPDATA01,
                sqlBuilder.buildPage(sqlFilteredBase, sqlOrder, pageSize, page),
            )
        }
        smxDb.callSql(withSocket = true)
        return smxDb.getRowset(TMPDATA01)
    }

    fun loadFieldMeta(objectType: String, objectName: String): Map<String, SmxDataPreviewFieldMeta> {
        val smxDb = buildDb()
        val tipoSql = sql(objectType.uppercase())
        val objetoSql = sql(objectName.uppercase())

        smxDb.addSTrans(
            TMPDATA03,
            "SELECT * FROM SP_PLUGIN_FIELDS('$tipoSql', '$objetoSql') " +
                "ORDER BY TMPFLD01_SECCION, TMPFLD01_POSICION",
        )
        smxDb.getCallWS(withSocket = true)

        val rowSet = smxDb.getRowset(TMPDATA03)
        val fields = mutableMapOf<String, SmxDataPreviewFieldMeta>()
        rowSet.beforeFirst()
        while (rowSet.next()) {
            val name = rowSet.getValue("TMPFLD01_CAMPO")?.toString().orEmpty()
            val section = rowSet.getValue("TMPFLD01_SECCION")?.toString().orEmpty()
            val dataType = rowSet.getValue("TMPFLD01_TIPO_DATO")?.toString().orEmpty()
            val length = (rowSet.getValue("TMPFLD01_LONGITUD") as? Number)?.toInt() ?: 0
            if (name.isNotBlank()) {
                fields[name.uppercase()] = SmxDataPreviewFieldMeta(name, dataType, section, length)
            }
        }
        return fields
    }

    fun loadDictionaryObjects(): List<SmxDictionaryObject> {
        val smxDb = buildDb()

        smxDb.addSTrans(
            TMPDATA04,
            "SELECT * FROM SP_PLUGIN_DB_CACHE ORDER BY TMPDICC01_ORDEN, TMPDICC01_NOMBRE",
        )
        smxDb.getCallWS(withSocket = true)

        val rowSet = smxDb.getRowset(TMPDATA04)
        val objects = mutableListOf<SmxDictionaryObject>()
        rowSet.beforeFirst()
        while (rowSet.next()) {
            val type = normalizeObjectType(rowSet.getValue("TMPDICC01_TIPO")?.toString().orEmpty()) ?: continue
            val name = rowSet.getValue("TMPDICC01_OBJETO")?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: rowSet.getValue("TMPDICC01_NOMBRE")?.toString().orEmpty()

            if (name.isNotBlank()) {
                objects.add(SmxDictionaryObject(type, name.uppercase()))
            }
        }
        return objects
    }

    private fun buildDb(): SmxDb {
        val smxDb = SmxDb { jsonSend, withSocket ->
            SmxJsonCall().getCallDBServer(jsonSend, withSocket = withSocket)
        }

        val profile = SmxSessionService.currentProfile()

        smxDb.jsonSend.usuario = profile.usuario.ifBlank { SmxConstantsCore.usuario }
        smxDb.jsonSend.clave = profile.clave.ifBlank { SmxConstantsCore.clave }
        smxDb.jsonSend.instancia = profile.instancia.ifBlank { SmxConstantsCore.instancia }
        smxDb.jsonSend.urlServidorWeb = profile.urlServidorWeb.ifBlank { SmxConstantsCore.urlServidorWeb }
        smxDb.jsonSend.uuidSession = profile.idSession.ifBlank { SmxConstantsCore.idSession }

        return smxDb
    }

    private fun sql(value: String): String =
        value.trim().replace("'", "''")

    private fun normalizeObjectType(value: String): String? {
        val normalized = value.trim().uppercase()
        return when {
            normalized == "TABLE" || normalized == "TABLA" || normalized == "T" -> "TABLE"
            normalized == "VIEW" || normalized == "VISTA" || normalized == "V" -> "VIEW"
            normalized == "PROCEDURE" || normalized == "PROCEDIMIENTO" || normalized == "P" || normalized == "SP" -> "PROCEDURE"
            normalized.contains("TABLE") || normalized.contains("TABLA") -> "TABLE"
            normalized.contains("VIEW") || normalized.contains("VISTA") -> "VIEW"
            normalized.contains("PROCED") -> "PROCEDURE"
            else -> null
        }
    }

    companion object {
        private const val TMPDATA01 = "TMPDATA01"
        private const val TMPDATA03 = "TMPDATA03"
        private const val TMPDATA04 = "TMPDATA04"
    }
}
