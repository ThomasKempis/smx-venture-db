package mx.jars.venture.venture_core_connect.smxDataBase

import mx.jars.venture.venture_core_connect.SmxConstantsCore
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonRow
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonSend
import mx.jars.venture.venture_core_connect.smxJson.SmxServicios

class SmxDb(
    private val callServer: ((SmxJsonSend, Boolean) -> SmxJsonSend)? = null,
) {
    private val sql: MutableMap<String, String> = mutableMapOf()
    private val idsParcing: MutableMap<String, Int> = mutableMapOf()
    private val sqlTrans: MutableMap<String, String> = mutableMapOf()
    private val rst: MutableMap<String, SmxRowSet> = mutableMapOf()

    var jsonSend: SmxJsonSend = SmxJsonSend()
        private set

    fun resetJsonSend() {
        jsonSend = SmxJsonSend()
    }

    fun begin() {
        sql.clear()
        rst.clear()
        idsParcing.clear()
        sqlTrans.clear()
    }

    fun addSql(key: String, sqlString: String, id: Int? = null) {
        val alias = key.uppercase()
        sql[alias] = sqlString

        if (id != null && id > 0) {
            idsParcing[alias] = id
        }

        jsonSend.microServicio = SmxServicios.CRUD_SQL.toStringValue()
    }

    fun addSqlParcing(key: String, sqlString: String, id: Int? = null) {
        val alias = key.uppercase()
        val sqlPage = "Select first ${SmxConstantsCore.rowsXpagina} ${formatSqlBase(sqlString)}"
        addSql(alias, sqlPage, id)
    }

    fun addSTrans(key: String, sqlString: String) {
        sqlTrans[key.uppercase()] = sqlString
        jsonSend.microServicio = SmxServicios.CRUD_TRANS.toStringValue()
    }

    fun getSql(key: String): String = sql[key.uppercase()].orEmpty()

    fun getRowset(key: String): SmxRowSet = rst[key.uppercase()] ?: SmxRowSet.empty()

    fun getRstCopy(key: String): List<SmxJsonRow>? = rst[key.uppercase()]?.getRows()?.toList()

    fun removeSql(key: String) {
        val alias = key.uppercase()
        sql.remove(alias)
        rst.remove(alias)
    }

    fun addRowset(key: String, sql: String, rst: SmxRowSet) {
        val alias = key.uppercase()
        this.sql[alias] = sql
        this.rst[alias] = rst
    }

    fun addOptions(key: String, rst: SmxRowSet) {
        addSql(key, "Select * from tmpoptions")
        this.rst[key.uppercase()] = rst
    }

    fun copyToRowset(aliasSource: String, aliasTarget: String) {
        val source = aliasSource.uppercase()
        val target = aliasTarget.uppercase()
        val rstSource = rst[source] ?: return

        rst[target] = rstSource.copy()
        sql[target] = rstSource.commandSql
    }

    fun getRowSetFuture(key: String, isFromCache: ((Boolean) -> Unit)? = null): List<SmxJsonRow> {
        val alias = key.uppercase()
        rst[alias]?.let {
            isFromCache?.invoke(true)
            return it.getRows()
        }

        isFromCache?.invoke(false)

        val sqlTmp = sql.filterKeys { !rst.containsKey(it) }.toMutableMap()
        val idsTmp = idsParcing.filterKeys { sqlTmp.containsKey(it) }.toMutableMap()

        jsonSend.error = null
        jsonSend.sql = sqlTmp.mapValues { it.value as Any? }.toMutableMap()
        jsonSend.idsParcing = idsTmp.mapValues { it.value as Any? }.toMutableMap()

        jsonSend = callServer?.invoke(jsonSend, true) ?: jsonSend

        if (jsonSend.error != null) {
            return listOf(SmxJsonRow(-1))
        }

        for (keySql in sqlTmp.keys) {
            rst[keySql] = SmxRowSet(
                alias = keySql,
                commandSql = sql[keySql].orEmpty(),
                rowset = jsonSend.getRowSetByKey(keySql).map { it.clone() }.toMutableList(),
                maxrows = jsonSend.getMaxRowsByKey(keySql),
                headers = jsonSend.getHeadersByKey(keySql).mapValues { it.value.clone() }.toMutableMap(),
            )
        }

        jsonSend.sql?.clear()
        jsonSend.rowsets?.clear()
        jsonSend.maxRows?.clear()
        jsonSend.fieldsHeader?.clear()

        return rst[alias]?.getRows().orEmpty()
    }

    fun callSql(withSocket: Boolean = true): SmxJsonSend {
        jsonSend.error = null
        jsonSend.sql = sql.mapValues { it.value as Any? }.toMutableMap()
        jsonSend.idsParcing = idsParcing.mapValues { it.value as Any? }.toMutableMap()
        jsonSend.microServicio = SmxServicios.CRUD_SQL.toStringValue()

        jsonSend = callServer?.invoke(jsonSend, withSocket) ?: jsonSend

        jsonSend.rowsets?.forEach { (key, rows) ->
            rst[key] = SmxRowSet(
                alias = key,
                commandSql = sql[key].orEmpty(),
                rowset = rows.map { it.clone() }.toMutableList(),
                maxrows = jsonSend.getMaxRowsByKey(key),
                headers = jsonSend.getHeadersByKey(key).mapValues { it.value.clone() }.toMutableMap(),
            )
        }

        return jsonSend
    }

    fun getCallWS(withSocket: Boolean = true): SmxJsonSend {
        if (sqlTrans.isNotEmpty()) {
            jsonSend.sql = sqlTrans.mapValues { it.value as Any? }.toMutableMap()
        }

        jsonSend = callServer?.invoke(jsonSend, withSocket) ?: jsonSend

        jsonSend.rowsets?.forEach { (key, rows) ->
            rst[key] = SmxRowSet(
                alias = key,
                commandSql = jsonSend.sql?.get(key)?.toString().orEmpty(),
                rowset = rows.map { it.clone() }.toMutableList(),
                maxrows = jsonSend.getMaxRowsByKey(key),
                headers = jsonSend.getHeadersByKey(key).mapValues { it.value.clone() }.toMutableMap(),
            )
        }

        return jsonSend
    }

    fun jsonSendToMap(): Map<String, Any?> {
        jsonSend.error = null
        jsonSend.sql = sql.mapValues { it.value as Any? }.toMutableMap()
        return jsonSend.toJson()
    }

    fun loadRowSetFromJsonSend(jsonSend: SmxJsonSend) {
        jsonSend.rowsets?.forEach { (key, rows) ->
            if (!rst.containsKey(key)) {
                rst[key] = SmxRowSet(
                    alias = key,
                    commandSql = jsonSend.sql?.get(key)?.toString().orEmpty(),
                    rowset = rows.map { it.clone() }.toMutableList(),
                    maxrows = jsonSend.getMaxRowsByKey(key),
                    headers = jsonSend.getHeadersByKey(key).mapValues { it.value.clone() }.toMutableMap(),
                )
            }
        }
    }

    private fun formatSqlBase(sqlString: String): String {
        var result = sqlString.uppercase().replaceFirst("SELECT", "").trim()
        result = result.replaceFirst("FIRST", "").trim()
        result = result.replaceFirst("SKIP", "").trim()
        result = result.replaceFirst(Regex("^\\d+"), "").trim()
        result = result.replaceFirst(Regex("^\\d+"), "").trim()
        return result
    }
}
