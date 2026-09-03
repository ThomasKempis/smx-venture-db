package mx.jars.venture.venture_core_connect.smxJson

data class SmxJsonRow(
    var posRow: Int,
    var accion: String = AccionNN.R.toJson(),
    var row: MutableMap<String, SmxJsonField> = mutableMapOf(),
    var rowDetTransaction: MutableList<SmxJsonRow> = mutableListOf(),
    var tabla: String? = null,
) {
    fun getId(): Any {
        for ((key, field) in row) {
            if (key.uppercase().startsWith("ID_")) {
                return when (field.tipoValor) {
                    TipoField.INT.toJson() -> field.valInt ?: -1
                    TipoField.LNG.toJson() -> field.valLng ?: -1
                    TipoField.STR.toJson() -> field.valStr.orEmpty()
                    else -> -1
                }
            }
        }
        return -1
    }

    fun toJson(): Map<String, Any?> =
        mapOf(
            "accion" to accion,
            "row" to row.mapValues { it.value.toJson() },
            "rowDetTransaction" to rowDetTransaction.map { it.toJson() },
            "posRow" to posRow,
            "tabla" to tabla,
        )

    fun clone(): SmxJsonRow =
        SmxJsonRow(
            posRow = posRow,
            accion = accion,
            row = row.mapValues { it.value.clone() }.toMutableMap(),
            rowDetTransaction = rowDetTransaction.map { it.clone() }.toMutableList(),
            tabla = tabla,
        )

    companion object {
        fun fromJson(json: Map<String, Any?>): SmxJsonRow {
            val rowMap = (json["row"] as? Map<*, *>).orEmpty()
                .mapNotNull { (key, value) ->
                    val fieldJson = value as? Map<String, Any?> ?: return@mapNotNull null
                    key.toString() to SmxJsonField.fromJson(fieldJson)
                }
                .toMap()
                .toMutableMap()

            val details = (json["rowDetTransaction"] as? List<*>).orEmpty()
                .mapNotNull { it as? Map<String, Any?> }
                .map { fromJson(it) }
                .toMutableList()

            return SmxJsonRow(
                posRow = (json["posRow"] as? Number)?.toInt() ?: -1,
                accion = json["accion"] as? String ?: AccionNN.R.toJson(),
                row = rowMap,
                rowDetTransaction = details,
                tabla = json["tabla"] as? String,
            )
        }
    }
}
