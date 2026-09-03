package mx.jars.venture.venture_core_connect.smxJson

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

data class SmxJsonField(
    var name: String?,
    var accion: String? = AccionNN.R.toJson(),
    var tipoValor: String = TipoField.NON.toJson(),
    var longitud: Int? = 0,
    var requerido: Boolean = false,
    var valStr: String? = null,
    var valDbl: Double? = null,
    var valInt: Int? = null,
    var valDate: LocalDateTime? = null,
    var valLng: Long? = null,
    var valLog: Boolean? = null,
    var valBytes: ByteArray? = null,
) {
    fun setTipo(tipo: String) {
        tipoValor = tipo
    }

    fun setValue(valor: Any?) {
        if (valor == null) return

        when (valor) {
            is Int -> setVal_int(valor)
            is Long -> setVal_lng(valor)
            is Double -> setVal_dbl(valor)
            is Float -> setVal_dbl(valor.toDouble())
            is Number -> setVal_dbl(valor.toDouble())
            is String -> setVal_str(valor)
            is Boolean -> setVal_log(valor)
            is LocalDateTime -> setVal_date(valor)
        }
    }

    fun setVal_str(valor: String) {
        accion = AccionNN.W.toJson()
        tipoValor = TipoField.STR.toJson()
        valStr = valor
    }

    fun setVal_dbl(valor: Double) {
        accion = AccionNN.W.toJson()
        tipoValor = TipoField.DBL.toJson()
        valDbl = valor
    }

    fun setVal_int(valor: Int) {
        accion = AccionNN.W.toJson()
        tipoValor = TipoField.INT.toJson()
        valInt = valor
    }

    fun setVal_lng(valor: Long) {
        accion = AccionNN.W.toJson()
        tipoValor = TipoField.LNG.toJson()
        valLng = valor
    }

    fun setVal_date(valor: LocalDateTime) {
        accion = AccionNN.W.toJson()
        tipoValor = TipoField.DAT.toJson()
        valDate = valor
    }

    fun setVal_log(valor: Boolean) {
        accion = AccionNN.W.toJson()
        tipoValor = TipoField.LOG.toJson()
        valLog = valor
    }

    fun setVal_null(tipoField: TipoField) {
        accion = AccionNN.W.toJson()
        tipoValor = tipoField.toJson()
        valStr = null
        valInt = null
        valLng = null
        valDbl = null
        valDate = null
        valLog = null
        valBytes = null
    }

    fun setVal_64(value: ByteArray) {
        valBytes = value
    }

    fun getObject(): Any? =
        when (tipoValor) {
            TipoField.LNG.toJson() -> valLng ?: valInt?.toLong() ?: valDbl?.toLong()
            TipoField.INT.toJson() -> valInt ?: valLng?.toInt() ?: valDbl?.toInt()
            TipoField.DBL.toJson() -> valDbl ?: valInt?.toDouble() ?: valLng?.toDouble()
            TipoField.DAT.toJson() -> valDate
            TipoField.STR.toJson() -> valStr
            TipoField.LOG.toJson() -> if (valLog == true) 1 else 0
            else -> null
        }

    fun toJson(): Map<String, Any?> =
        mapOf(
            "name" to name,
            "accion" to accion,
            "tipoValor" to tipoValor,
            "longitud" to longitud,
            "requerido" to requerido,
            "val_str" to valStr,
            "val_dbl" to valDbl,
            "val_int" to valInt,
            "val_date" to valDate?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "val_lng" to valLng,
            "val_log" to valLog,
            "val_bytes" to valBytes?.toList(),
        )

    fun clone(): SmxJsonField = copy(valBytes = valBytes?.copyOf())

    override fun toString(): String =
        when (tipoValor) {
            TipoField.LNG.toJson() -> valLng?.toString().orEmpty()
            TipoField.INT.toJson() -> valInt?.toString().orEmpty()
            TipoField.DBL.toJson() -> valDbl?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()
            TipoField.DAT.toJson() -> valDate?.format(DateTimeFormatter.ofPattern("d MMM y H:mm", Locale("es"))).orEmpty()
            TipoField.STR.toJson() -> valStr.orEmpty()
            TipoField.LOG.toJson() -> if (valLog == true) "No" else "Si"
            else -> ""
        }

    companion object {
        fun string(name: String, value: String) = SmxJsonField(name = name, valStr = value, tipoValor = TipoField.STR.toJson())
        fun double(name: String, value: Double) = SmxJsonField(name = name, valDbl = value, tipoValor = TipoField.DBL.toJson())
        fun int(name: String, value: Int) = SmxJsonField(name = name, valInt = value, tipoValor = TipoField.INT.toJson())
        fun date(name: String, value: LocalDateTime) = SmxJsonField(name = name, valDate = value, tipoValor = TipoField.DAT.toJson())
        fun long(name: String, value: Long) = SmxJsonField(name = name, valLng = value, tipoValor = TipoField.LNG.toJson())
        fun bool(name: String, value: Boolean) = SmxJsonField(name = name, valLog = value, tipoValor = TipoField.LOG.toJson())
        fun binary(name: String, value: ByteArray) = SmxJsonField(name = name, valBytes = value, tipoValor = TipoField.BIN.toJson())

        fun fromJson(json: Map<String, Any?>): SmxJsonField {
            val bytes = (json["val_bytes"] as? List<*>)?.mapNotNull { (it as? Number)?.toByte() }?.toByteArray()
            return SmxJsonField(
                name = json["name"] as? String,
                accion = json["accion"] as? String,
                tipoValor = json["tipoValor"] as? String ?: TipoField.NON.toJson(),
                longitud = (json["longitud"] as? Number)?.toInt(),
                requerido = json["requerido"] as? Boolean ?: false,
                valStr = json["val_str"]?.toString(),
                valDbl = (json["val_dbl"] as? Number)?.toDouble(),
                valInt = (json["val_int"] as? Number)?.toInt(),
                valDate = parseDate(json["val_date"] as? String),
                valLng = (json["val_lng"] as? Number)?.toLong(),
                valLog = json["val_log"] as? Boolean,
                valBytes = bytes,
            )
        }

        private fun parseDate(value: String?): LocalDateTime? {
            if (value.isNullOrBlank()) return null
            val normalized = normalizeDateText(value)
            val serverFormats = listOf(
                "MMM d, yyyy h:mm:ss a",
                "MMM d, yyyy hh:mm:ss a",
            ).map { pattern ->
                DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .toFormatter(Locale.US)
            }
            val formats = serverFormats + DateTimeFormatter.ISO_LOCAL_DATE_TIME
            return formats.firstNotNullOfOrNull { formatter ->
                runCatching { LocalDateTime.parse(normalized, formatter) }.getOrNull()
            }
        }

        private fun normalizeDateText(value: String): String =
            value.trim()
                .replace(Regex("\\ba\\.\\s*m\\.\\b", RegexOption.IGNORE_CASE), "AM")
                .replace(Regex("\\bp\\.\\s*m\\.\\b", RegexOption.IGNORE_CASE), "PM")
                .replace(Regex("\\bEne\\.?\\b", RegexOption.IGNORE_CASE), "Jan")
                .replace(Regex("\\bAbr\\.?\\b", RegexOption.IGNORE_CASE), "Apr")
                .replace(Regex("\\bAgo\\.?\\b", RegexOption.IGNORE_CASE), "Aug")
                .replace(Regex("\\bSept\\.?\\b", RegexOption.IGNORE_CASE), "Sep")
                .replace(Regex("\\bSet\\.?\\b", RegexOption.IGNORE_CASE), "Sep")
                .replace(Regex("\\bOct\\.?\\b", RegexOption.IGNORE_CASE), "Oct")
                .replace(Regex("\\bNov\\.?\\b", RegexOption.IGNORE_CASE), "Nov")
                .replace(Regex("\\bDic\\.?\\b", RegexOption.IGNORE_CASE), "Dec")
    }
}
