package mx.jars.venture.venture_core_connect.smxJson

import java.time.LocalDateTime

class SmxJsonChat(
    var jsonSend: SmxJsonSend? = null,
    var jsonGeo: SmxJsonGeo? = null,
    var messege: String = "",
    var correoDestino: String = "",
    var loadMessagePendding: Boolean = false,
    var isSendMessage: Boolean = false,
    var isRecibeMessage: Boolean = false,
    idSysUser10: Int = -1,
    correo: String = "",
    nombre: String = "",
    locacion: String = "",
    fecha: LocalDateTime = LocalDateTime.now(),
) : SMXJsonGeoBase(idSysUser10, correo, nombre, locacion, fecha) {
    override fun toJson(): MutableMap<String, Any?> =
        super.toJson().apply {
            put("jsonSend", jsonSend?.toJson())
            put("jsonGeo", jsonGeo?.toJson())
            put("messege", messege)
            put("correoDestino", correoDestino)
            put("isSendMessage", isSendMessage)
            put("loadMessagePendding", loadMessagePendding)
            put("isRecibeMessage", isRecibeMessage)
        }

    companion object {
        fun fromJson(json: Map<String, Any?>): SmxJsonChat =
            SmxJsonChat(
                jsonSend = (json["jsonSend"] as? Map<String, Any?>)?.let { SmxJsonSend.fromJson(it) },
                jsonGeo = (json["jsonGeo"] as? Map<String, Any?>)?.let { SmxJsonGeo.fromJson(it) },
                messege = json["messege"] as? String ?: "",
                correoDestino = json["correoDestino"] as? String ?: "",
                loadMessagePendding = json["loadMessagePendding"] as? Boolean ?: false,
                isSendMessage = json["isSendMessage"] as? Boolean ?: false,
                isRecibeMessage = json["isRecibeMessage"] as? Boolean ?: false,
                idSysUser10 = (json["idSysUser10"] as? Number)?.toInt() ?: -1,
                correo = json["correo"] as? String ?: "",
                nombre = json["nombre"] as? String ?: "",
                locacion = json["locacion"] as? String ?: "",
                fecha = parseChatDate(json["fecha"] as? String),
            )
    }
}

private fun parseChatDate(value: String?): LocalDateTime {
    if (value.isNullOrBlank()) return LocalDateTime.now()
    return runCatching { LocalDateTime.parse(value) }.getOrElse { LocalDateTime.now() }
}
