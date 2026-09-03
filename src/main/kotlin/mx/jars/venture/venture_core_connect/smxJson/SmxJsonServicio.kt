package mx.jars.venture.venture_core_connect.smxJson

import java.time.LocalDateTime

data class SmxJsonServicio(
    var accion: String = AccionNN.I.toJson(),
    var variables: MutableMap<String, SmxJsonField> = mutableMapOf(),
    var subServicio: String = "NONE",
) {
    fun setVariable(name: String, variable: Any?, tipoVar: TipoField) {
        val key = name.uppercase()
        val field = when {
            variable is String && tipoVar == TipoField.STR -> SmxJsonField.string(key, variable)
            variable is Int && tipoVar == TipoField.INT -> SmxJsonField.int(key, variable)
            variable is Long && tipoVar == TipoField.LNG -> SmxJsonField.long(key, variable)
            variable is Double && tipoVar == TipoField.DBL -> SmxJsonField.double(key, variable)
            variable is LocalDateTime && tipoVar == TipoField.DAT -> SmxJsonField.date(key, variable)
            variable is Boolean && tipoVar == TipoField.LOG -> SmxJsonField.bool(key, variable)
            variable is ByteArray && tipoVar == TipoField.BIN -> SmxJsonField.binary(key, variable)
            else -> SmxJsonField(name = key)
        }
        variables[key] = field
    }

    fun getValorBytes(name: String): ByteArray? = variables[name]?.valBytes
    fun getValorStr(name: String): String? = variables[name]?.valStr
    fun getValorInt(name: String): Int? = variables[name]?.valInt
    fun getValorLng(name: String): Long? = variables[name]?.valLng
    fun getValorDbl(name: String): Double? = variables[name]?.valDbl
    fun getValorLog(name: String): Boolean? = variables[name]?.valLog
    fun getValorDate(name: String): LocalDateTime? = variables[name]?.valDate

    fun toJson(): Map<String, Any?> =
        mapOf(
            "accion" to accion,
            "variables" to variables.mapValues { it.value.toJson() },
            "subServicio" to subServicio,
        )

    fun clone(): SmxJsonServicio =
        SmxJsonServicio(
            accion = accion,
            variables = variables.mapValues { it.value.clone() }.toMutableMap(),
            subServicio = subServicio,
        )

    companion object {
        fun fromJson(json: Map<String, Any?>): SmxJsonServicio {
            val fields = (json["variables"] as? Map<*, *>).orEmpty()
                .mapNotNull { (key, value) ->
                    val fieldJson = value as? Map<String, Any?> ?: return@mapNotNull null
                    key.toString() to SmxJsonField.fromJson(fieldJson)
                }
                .toMap()
                .toMutableMap()

            return SmxJsonServicio(
                accion = json["accion"] as? String ?: AccionNN.I.toJson(),
                variables = fields,
                subServicio = json["subServicio"] as? String ?: "NONE",
            )
        }
    }
}
