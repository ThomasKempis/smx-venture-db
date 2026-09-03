package mx.jars.venture.venture_core_connect.smxDataBase

import mx.jars.venture.venture_core_connect.SmxConstantsCore
import mx.jars.venture.venture_core_connect.smxJson.AccionNN
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonField
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonRow
import mx.jars.venture.venture_core_connect.smxJson.TipoField
import java.time.LocalDateTime

enum class Sort {
    ASC,
    DESC,
}

class SmxRowSet(
    val alias: String,
    val commandSql: String,
    private val rowset: MutableList<SmxJsonRow>,
    private val maxrows: Int,
    private val headers: MutableMap<String, SmxJsonField>,
) {
    var tabla: String = ""
    var indexPos: Int = -1
        private set

    private var rowActivo: SmxJsonRow? = null
    private val detTransaccion: MutableMap<String, SmxRowSet> = mutableMapOf()

    fun getRows(): List<SmxJsonRow> = rowset.filter { it.accion != AccionNN.D.toJson() }

    fun getRowsList(): List<SmxJsonRow> = rowset

    fun getRowsABM(): List<SmxJsonRow> =
        rowset.filter {
            it.accion == AccionNN.W.toJson() ||
                it.accion == AccionNN.D.toJson() ||
                it.accion == AccionNN.I.toJson()
        }

    fun getMaxRows(): Int = maxrows

    fun getRowSetFind(field: String, value: Any?): SmxJsonRow {
        val key = field.uppercase()
        return rowset.firstOrNull { it.row.containsKey(key) && it.row[key].toString() == value.toString() }
            ?: SmxJsonRow(-1)
    }

    fun getFieldsHeader(key: String): SmxJsonField =
        headers[key.uppercase()] ?: SmxJsonField(name = "NONE")

    fun copy(): SmxRowSet =
        SmxRowSet(
            alias = alias,
            commandSql = commandSql,
            rowset = rowset.map { it.clone() }.toMutableList(),
            maxrows = maxrows,
            headers = headers.mapValues { it.value.clone() }.toMutableMap(),
        ).also { it.tabla = tabla }

    fun sort(field: String, sort: Sort = Sort.ASC) {
        val key = field.uppercase()
        if (sort == Sort.ASC) {
            rowset.sortBy { it.row[key]?.valInt ?: -1 }
        } else {
            rowset.sortByDescending { it.row[key]?.valInt ?: -1 }
        }
        first()
    }

    fun isEmptyData(): Boolean = getRowCount() == 0

    fun getRowCount(): Int = rowset.size

    fun getRowCountSinDelete(): Int = getRows().size

    fun beforeFirst() {
        indexPos = -1
        rowActivo = null
    }

    fun first() {
        if (isEmpty()) return
        indexPos = 0
        rowActivo = rowset[indexPos]
    }

    fun last() {
        if (isEmpty()) return
        indexPos = rowset.size - 1
        rowActivo = rowset[indexPos]
    }

    fun next(conDeletes: Boolean = true): Boolean {
        if (isEmpty()) return false

        indexPos++
        if (indexPos > rowset.size - 1) {
            indexPos--
            return false
        }

        rowActivo = rowset[indexPos]

        if (!conDeletes && rowActivo?.accion == AccionNN.D.toJson()) {
            for (irow in indexPos until rowset.size) {
                indexPos = irow
                rowActivo = rowset[irow]
                if (rowActivo?.accion != AccionNN.D.toJson()) return true
            }
            return false
        }

        return true
    }

    fun index(intPosi: Int) {
        if (isEmpty()) return
        if (intPosi !in rowset.indices) return
        indexPos = intPosi
        rowActivo = rowset[indexPos]
    }

    fun isDelete(): Boolean = !isEmpty() && getRow().accion == AccionNN.D.toJson()

    fun copyActive(): SmxRowSet {
        val active = rowActivo ?: return empty()
        return SmxRowSet(
            alias = alias,
            commandSql = commandSql,
            rowset = mutableListOf(active.clone()),
            maxrows = 1,
            headers = headers.mapValues { it.value.clone() }.toMutableMap(),
        ).also {
            it.tabla = tabla
            it.first()
        }
    }

    fun getRow(): SmxJsonRow = rowActivo ?: SmxJsonRow(-1)

    fun findIdRow(valueId: Int): SmxJsonRow {
        val field = getIdFieldName()
        return rowset.firstOrNull { (it.row[field]?.valLng ?: it.row[field]?.valInt?.toLong()) == valueId.toLong() }
            ?: SmxJsonRow(-1)
    }

    fun findId(value: Int): Boolean {
        val field = getIdFieldName()
        for (i in rowset.indices) {
            val row = rowset[i]
            val id = row.row[field]?.valLng ?: row.row[field]?.valInt?.toLong() ?: -1
            if (id == value.toLong()) {
                indexPos = i
                rowActivo = rowset[indexPos]
                return true
            }
        }
        return false
    }

    fun find(field: String, value: Any?): Boolean {
        val key = field.uppercase()
        for (i in rowset.indices) {
            val row = rowset[i]
            if (row.row.containsKey(key) && row.row[key].toString() == value.toString()) {
                indexPos = i
                rowActivo = rowset[indexPos]
                return true
            }
        }
        return false
    }

    fun getFullRow(): Map<String, SmxJsonField> = rowActivo?.row.orEmpty()

    fun getValue(field: String): Any? {
        val active = rowActivo
        if (active == null) {
            val header = headers[field]
            return if (header?.tipoValor == TipoField.STR.toJson()) "" else if (header == null) null else -1
        }

        val key = field.uppercase()
        val jsonField = active.row[key] ?: return null
        return getValue(jsonField.getObject(), jsonField.tipoValor)
    }

    fun getValueFormatAmount(field: String): String = formatAmount(getValue(field))

    fun getValueFormatDate(field: String): String = formatDate(getValue(field))

    fun getAccion(): AccionNN = AccionNN.fromString(rowActivo?.accion)

    fun setAccion(accion: AccionNN) {
        rowActivo?.accion = accion.toJson()
    }

    fun delete(removeBuffer: Boolean = true) {
        val active = rowActivo ?: return

        val id = getId()
        if (id <= 0 && removeBuffer) {
            rowset.removeAt(indexPos)
            active.row.clear()
            SmxConstantsCore.removeRowHistory(alias = alias, id = id)
            detTransaccion.remove(alias)
        }

        active.accion = AccionNN.D.toJson()
        active.tabla = tabla
        active.rowDetTransaction.clear()
    }

    fun deleteBuffer() {
        if (indexPos in rowset.indices) {
            rowset.removeAt(indexPos)
        }
    }

    fun update(field: String, value: Any?) {
        val active = rowActivo ?: return
        val key = field.uppercase()
        val jsonField = active.row[key] ?: return
        val tipoField = TipoField.fromString(jsonField.tipoValor)

        active.tabla = tabla
        if (active.accion == AccionNN.R.toJson() || active.accion == AccionNN.N.toJson()) {
            active.accion = AccionNN.W.toJson()
        }

        if (value == null && tipoField != TipoField.DAT) {
            jsonField.setVal_null(tipoField)
            return
        }

        when (tipoField) {
            TipoField.INT -> jsonField.setVal_int(getValue(value, tipoField.toJson()) as Int)
            TipoField.LNG -> jsonField.setVal_lng((getValue(value, tipoField.toJson()) as Number).toLong())
            TipoField.DBL -> jsonField.setVal_dbl(getValue(value, tipoField.toJson()) as Double)
            TipoField.STR -> jsonField.setVal_str(getValue(value, tipoField.toJson()) as String)
            TipoField.DAT -> {
                val dateValue = getValue(value, tipoField.toJson()) as? LocalDateTime
                if (dateValue == null) jsonField.setVal_null(TipoField.DAT) else jsonField.setVal_date(dateValue)
            }
            TipoField.LOG -> jsonField.setVal_log(getValue(value, tipoField.toJson()) as Boolean)
            else -> jsonField.setVal_64("Error: sin tipo asignado de variable".toByteArray())
        }
    }

    fun insert() {
        val rowsetAdd = SmxJsonRow(getRowCount())
        val headersCopy = headers.mapValues { it.value.clone().also { field ->
            field.valStr = null
            field.valDbl = null
            field.valInt = null
            field.valDate = null
            field.valLng = null
            field.valLog = null
            field.valBytes = null
        } }.toMutableMap()

        rowsetAdd.row = headersCopy
        rowsetAdd.accion = AccionNN.I.toJson()
        rowsetAdd.tabla = tabla
        rowset.add(rowsetAdd)

        last()
        setId()
    }

    fun setDetTransaction(rowsetRec: SmxRowSet) {
        val active = rowActivo ?: return
        detTransaccion[rowsetRec.alias.uppercase()] = rowsetRec

        first()
        val intPosi = rowsetRec.getId()

        rowsetRec.beforeFirst()
        while (rowsetRec.next()) {
            var write = true
            var baja = false
            var intRow = 0

            if (rowsetRec.getAccion() == AccionNN.N || rowsetRec.getAccion() == AccionNN.R) {
                intRow++
                continue
            }

            for (row in active.rowDetTransaction) {
                val rowTabla = row.tabla.orEmpty()
                val rowsetRecTabla = rowsetRec.tabla

                if (rowTabla.isEmpty() || rowsetRecTabla.isEmpty()) {
                    write = false
                    break
                }

                if (row.getId() == rowsetRec.getId() && rowTabla == rowsetRecTabla) {
                    write = false
                    if (rowsetRec.getRow().row.isEmpty() || (rowsetRec.getAccion() == AccionNN.D && rowsetRec.getId() <= 0)) {
                        baja = true
                    } else {
                        rowset[indexPos].rowDetTransaction[intRow] = rowsetRec.getRow()
                    }
                    break
                }
                intRow++
            }

            if (write) {
                val rowRec = rowsetRec.getRow()
                rowRec.tabla = rowsetRec.tabla
                rowset[indexPos].rowDetTransaction.add(rowRec)
            } else if (baja) {
                rowset[indexPos].rowDetTransaction.removeAt(intRow)
            }
        }

        rowsetRec.findId(intPosi)
    }

    fun getDetTransaction(alias: String): SmxRowSet? =
        if (rowActivo == null) null else detTransaccion[alias.uppercase()]

    fun getRowDetTransaction(): List<SmxJsonRow> = rowActivo?.rowDetTransaction.orEmpty()

    fun getId(): Int {
        val active = rowActivo ?: return -1
        if (active.row.isEmpty()) return -1
        val field = active.row[getIdFieldName()] ?: return -1
        return field.valLng?.toInt() ?: field.valInt ?: --SmxConstantsCore.idFalse
    }

    fun getFapl(): LocalDateTime {
        val active = rowActivo ?: return LocalDateTime.now()
        return active.row[getFaplFieldName()]?.valDate ?: LocalDateTime.now()
    }

    fun setId() {
        val active = rowActivo ?: return
        active.row[getIdFieldName()]?.valLng = (--SmxConstantsCore.idFalse).toLong()
        active.row[getFaplFieldName()]?.valDate = LocalDateTime.now()
    }

    fun getIdFieldName(): String =
        headers.keys.firstOrNull { it.uppercase().startsWith("ID_") }.orEmpty()

    fun getFaplFieldName(): String =
        headers.keys.firstOrNull { it.uppercase().endsWith("_FAPL") }.orEmpty()

    private fun isEmpty(): Boolean {
        if (rowset.isEmpty()) {
            beforeFirst()
            return true
        }
        return false
    }

    private fun getValue(value: Any?, tipoCampo: String): Any? =
        when (TipoField.fromString(tipoCampo)) {
            TipoField.LNG,
            TipoField.INT -> when (value) {
                null, "" -> 0
                is Int -> value
                is Long -> value.toInt()
                is Double -> value.toInt()
                is Number -> value.toInt()
                is String -> value.toDoubleOrNull()?.toInt() ?: 0
                is Boolean -> if (value) 1 else 0
                else -> 0
            }

            TipoField.DBL -> when (value) {
                null, "" -> 0.0
                is Double -> value
                is Int -> value.toDouble()
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            TipoField.DAT -> value

            TipoField.STR -> when (value) {
                is Boolean -> if (value) "Si" else "No"
                is Double, is Int -> value.toString()
                else -> value as? String ?: ""
            }

            TipoField.LOG -> when (value) {
                is Boolean -> value
                is Int -> value != 0
                is Number -> value.toInt() != 0
                else -> false
            }

            else -> null
        }

    companion object {
        fun empty(): SmxRowSet =
            SmxRowSet(alias = "", commandSql = "", rowset = mutableListOf(), maxrows = 0, headers = mutableMapOf())

        fun newRowSet(rows: List<Map<String, Any?>>, alias: String = "TMP_ROWSET"): SmxRowSet {
            val setRows = mutableListOf<SmxJsonRow>()
            var fieldsCab = mutableMapOf<String, SmxJsonField>()

            rows.forEachIndexed { index, rowUser ->
                val row = SmxJsonRow(index)
                val fields = mutableMapOf<String, SmxJsonField>()

                for ((fieldName, rawValue) in rowUser) {
                    val field = fieldName.uppercase().trim()
                    fields[field] = if (rawValue is SmxJsonField) {
                        rawValue.clone()
                    } else {
                        SmxJsonField(name = field).also { it.setValue(rawValue) }
                    }
                    row.row[field] = fields[field]!!
                }

                if (fieldsCab.isEmpty()) {
                    fieldsCab = fields
                }
                setRows.add(row)
            }

            return SmxRowSet(
                alias = alias,
                commandSql = "Select * from none",
                rowset = setRows,
                maxrows = setRows.size + 1,
                headers = fieldsCab,
            )
        }

        fun newRowSetCopyRow(smxJsonRow: SmxJsonRow): SmxRowSet =
            newRowSet(listOf(smxJsonRow.row)).also { it.first() }

        fun formatAmount(value: Any?): String {
            val amount = amountFrom(value)
            val sign = if (amount < 0) "-" else ""
            val fixed = kotlin.math.abs(amount).toLong().toString()
            val parts = mutableListOf<String>()

            var end = fixed.length
            while (end > 0) {
                val start = if (end - 3 > 0) end - 3 else 0
                parts.add(0, fixed.substring(start, end))
                end -= 3
            }

            return "$sign$${parts.joinToString(",")}"
        }

        private fun amountFrom(value: Any?): Double {
            if (value is Number) return value.toDouble()
            val text = value?.toString()?.replace("$", "")?.replace(",", "")?.trim().orEmpty()
            return text.toDoubleOrNull() ?: 0.0
        }

        fun formatDate(value: Any?): String {
            val date = when (value) {
                is LocalDateTime -> value
                is String -> runCatching { LocalDateTime.parse(value) }.getOrNull()
                else -> null
            } ?: return ""

            return "${date.dayOfMonth.toString().padStart(2, '0')}/" +
                "${date.monthValue.toString().padStart(2, '0')}/" +
                "${date.year}"
        }
    }
}
