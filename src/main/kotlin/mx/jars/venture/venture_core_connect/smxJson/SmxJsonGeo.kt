package mx.jars.venture.venture_core_connect.smxJson

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

open class SMXJsonGeoBase(
    var idSysUser10: Int = -1,
    var correo: String = "",
    var nombre: String = "",
    var locacion: String = "",
    var fecha: LocalDateTime = LocalDateTime.now(),
) {
    open fun toJson(): MutableMap<String, Any?> =
        mutableMapOf(
            "idSysUser10" to idSysUser10,
            "correo" to correo,
            "nombre" to nombre,
            "locacion" to locacion,
            "fecha" to fecha.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        )
}

class SmxJsonGeo(
    idSysUser10: Int = -1,
    correo: String = "",
    nombre: String = "",
    locacion: String = "",
    fecha: LocalDateTime = LocalDateTime.now(),
    var jsonSend: SmxJsonSend? = null,
) : SMXJsonGeoBase(idSysUser10, correo, nombre, locacion, fecha) {
    val invitados: MutableMap<String, SMXJsonGeoUsuario> = mutableMapOf()
    val invitadosAceptados: MutableMap<String, SMXJsonGeoUsuario> = mutableMapOf()

    fun addInvitado(correo: String, nombre: String) {
        val key = correo.lowercase()
        invitados[key] = SMXJsonGeoUsuario(correo = key, nombre = nombre.uppercase())
    }

    override fun toJson(): MutableMap<String, Any?> =
        super.toJson().apply {
            put("jsonSend", jsonSend?.toJson())
            put("invitados", invitados.mapValues { it.value.toJson() })
            put("invitadosAceptados", invitadosAceptados.mapValues { it.value.toJson() })
        }

    companion object {
        fun fromJson(json: Map<String, Any?>): SmxJsonGeo {
            val geo = SmxJsonGeo(
                idSysUser10 = (json["idSysUser10"] as? Number)?.toInt() ?: -1,
                correo = json["correo"] as? String ?: "",
                nombre = json["nombre"] as? String ?: "",
                locacion = json["locacion"] as? String ?: "",
                fecha = parseGeoDate(json["fecha"] as? String),
                jsonSend = (json["jsonSend"] as? Map<String, Any?>)?.let { SmxJsonSend.fromJson(it) },
            )

            (json["invitados"] as? Map<*, *>)?.forEach { (key, value) ->
                val userJson = value as? Map<String, Any?> ?: return@forEach
                geo.invitados[key.toString()] = SMXJsonGeoUsuario.fromJson(userJson)
            }

            (json["invitadosAceptados"] as? Map<*, *>)?.forEach { (key, value) ->
                val userJson = value as? Map<String, Any?> ?: return@forEach
                geo.invitadosAceptados[key.toString()] = SMXJsonGeoUsuario.fromJson(userJson)
            }

            return geo
        }
    }
}

class SMXJsonGeoUsuario(
    idSysUser10: Int = -1,
    correo: String = "",
    nombre: String = "",
    locacion: String = "",
    fecha: LocalDateTime = LocalDateTime.now(),
) : SMXJsonGeoBase(idSysUser10, correo, nombre, locacion, fecha) {
    companion object {
        fun fromJson(json: Map<String, Any?>): SMXJsonGeoUsuario =
            SMXJsonGeoUsuario(
                idSysUser10 = (json["idSysUser10"] as? Number)?.toInt() ?: -1,
                correo = json["correo"] as? String ?: "",
                nombre = json["nombre"] as? String ?: "",
                locacion = json["locacion"] as? String ?: "",
                fecha = parseGeoDate(json["fecha"] as? String),
            )
    }
}

private fun parseGeoDate(value: String?): LocalDateTime {
    if (value.isNullOrBlank()) return LocalDateTime.now()
    return runCatching { LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
        .getOrElse { LocalDateTime.now() }
}
