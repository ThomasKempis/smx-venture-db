package mx.jars.venture.venture_core_connect.smxJson

import mx.jars.venture.venture_core_connect.SmxConstantsCore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class SmxJsonSend(
    var usuario: String = SmxConstantsCore.usuario,
    var clave: String = SmxConstantsCore.clave,
    var instancia: String = SmxConstantsCore.instancia,
    var idEmpresa: Int = SmxConstantsCore.idEmpresa,
    var urlServidorWeb: String = SmxConstantsCore.urlServidorWeb,
    var idUsuario: Int = SmxConstantsCore.idUsuario,
    var idCabOrga: Int = SmxConstantsCore.idCabOrga,
    var idDetDepa: Int = SmxConstantsCore.idDetDepa,
    var regimenFiscal: String = SmxConstantsCore.regimenFiscal,
    var rfcEmpresa: String = SmxConstantsCore.rfcEmpresa,
    var dateServidor: LocalDateTime? = null,
    var uuidConnect: String = UUID.randomUUID().toString(),
    var uuidSession: String = SmxConstantsCore.idSession,
    var microServicio: String? = null,
    var status: String? = null,
    var statuscode: String? = null,
    var error: String? = null,
    var help: String? = null,
    var sql: MutableMap<String, Any?>? = null,
    var maxRows: MutableMap<String, Any?>? = null,
    var idsParcing: MutableMap<String, Any?>? = null,
    var rowsets: MutableMap<String, MutableList<SmxJsonRow>>? = null,
    var fieldsHeader: MutableMap<String, MutableMap<String, Any?>>? = null,
    var files: MutableMap<String, SmxJsonFiles>? = null,
    var rowTransaction: SmxJsonTransaction? = null,
    var rowsABM: MutableList<SmxJsonRow>? = null,
    var servicio: SmxJsonServicio? = null,
    var imagenPerfil: MutableList<Int>? = null,
) {
    fun getSqlByKey(key: String): String = sql?.get(key)?.toString().orEmpty()

    fun getRowSetByKey(key: String): List<SmxJsonRow> = rowsets?.get(key).orEmpty()

    fun getMaxRowsByKey(key: String): Int = (maxRows?.get(key) as? Number)?.toInt() ?: 0

    fun getHeadersByKey(key: String): Map<String, SmxJsonField> {
        val fieldValue = fieldsHeader?.get(key).orEmpty()
        return fieldValue.mapNotNull { (entryKey, entryValue) ->
            val json = entryValue as? Map<String, Any?> ?: return@mapNotNull null
            entryKey to SmxJsonField.fromJson(json)
        }.toMap()
    }

    fun toJson(): Map<String, Any?> =
        mapOf(
            "usuario" to usuario,
            "clave" to clave,
            "instancia" to instancia,
            "idEmpresa" to idEmpresa,
            "urlServidorWeb" to urlServidorWeb,
            "idUsuario" to idUsuario,
            "dateServidor" to dateServidor?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "idCabOrga" to idCabOrga,
            "idDetDepa" to idDetDepa,
            "regimenFiscal" to regimenFiscal,
            "rfcEmpresa" to rfcEmpresa,
            "uuidConnect" to uuidConnect,
            "uuidSession" to uuidSession,
            "microServicio" to microServicio,
            "status" to status,
            "statuscode" to statuscode,
            "error" to error,
            "help" to help,
            "sql" to sql,
            "maxRows" to maxRows,
            "idsParcing" to idsParcing,
            "rowsets" to rowsets?.mapValues { it.value.map { row -> row.toJson() } }.orEmpty(),
            "fieldsHeader" to fieldsHeader.orEmpty(),
            "rowsABM" to rowsABM?.map { it.toJson() },
            "rowTransaction" to rowTransaction?.toJson(),
            "files" to files?.mapValues { it.value.toJson() },
            "servicio" to servicio?.toJson(),
            "imagenPerfil" to imagenPerfil,
        )

    fun clone(): SmxJsonSend =
        copy(
            sql = sql?.toMutableMap(),
            maxRows = maxRows?.toMutableMap(),
            idsParcing = idsParcing?.toMutableMap(),
            rowsets = rowsets?.mapValues { it.value.map { row -> row.clone() }.toMutableList() }?.toMutableMap(),
            fieldsHeader = fieldsHeader?.mapValues { it.value.toMutableMap() }?.toMutableMap(),
            files = files?.toMutableMap(),
            rowTransaction = rowTransaction?.clone(),
            rowsABM = rowsABM?.map { it.clone() }?.toMutableList(),
            servicio = servicio?.clone(),
            imagenPerfil = imagenPerfil?.toMutableList(),
        )

    companion object {
        fun fromJson(json: Map<String, Any?>): SmxJsonSend =
            SmxJsonSend(
                usuario = json["usuario"] as? String ?: "INVITADO",
                clave = json["clave"] as? String ?: "INVITADO",
                instancia = json["instancia"] as? String ?: "",
                idEmpresa = (json["idEmpresa"] as? Number)?.toInt() ?: -1,
                urlServidorWeb = json["urlServidorWeb"] as? String ?: "",
                idUsuario = (json["idUsuario"] as? Number)?.toInt() ?: -1,
                idCabOrga = (json["idCabOrga"] as? Number)?.toInt() ?: -1,
                idDetDepa = (json["idDetDepa"] as? Number)?.toInt() ?: -1,
                regimenFiscal = json["regimenFiscal"] as? String ?: "601",
                rfcEmpresa = json["rfcEmpresa"] as? String ?: "XAXX010101000",
                dateServidor = parseDate(json["dateServidor"] as? String),
                uuidConnect = json["uuidConnect"] as? String ?: UUID.randomUUID().toString(),
                uuidSession = json["uuidSession"] as? String ?: UUID.randomUUID().toString(),
                microServicio = json["microServicio"] as? String,
                status = json["status"] as? String,
                statuscode = json["statuscode"] as? String,
                error = json["error"] as? String,
                help = json["help"] as? String,
                sql = (json["sql"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.toMutableMap(),
                maxRows = (json["maxRows"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.toMutableMap(),
                idsParcing = (json["idsParcing"] as? Map<*, *>)?.mapKeys { it.key.toString() }?.toMutableMap(),
                rowsets = parseRowsets(json["rowsets"]),
                fieldsHeader = parseFieldsHeader(json["fieldsHeader"]),
                rowsABM = (json["rowsABM"] as? List<*>)?.mapNotNull { it as? Map<String, Any?> }?.map { SmxJsonRow.fromJson(it) }?.toMutableList(),
                rowTransaction = (json["rowTransaction"] as? Map<String, Any?>)?.let { SmxJsonTransaction.fromJson(it) },
                files = (json["files"] as? Map<*, *>)?.mapNotNull { (key, value) ->
                    val fileJson = value as? Map<String, Any?> ?: return@mapNotNull null
                    key.toString() to SmxJsonFiles.fromJson(fileJson)
                }?.toMap()?.toMutableMap(),
                servicio = (json["servicio"] as? Map<String, Any?>)?.let { SmxJsonServicio.fromJson(it) },
                imagenPerfil = (json["imagenPerfil"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }?.toMutableList(),
            )

        private fun parseRowsets(value: Any?): MutableMap<String, MutableList<SmxJsonRow>>? =
            (value as? Map<*, *>)?.mapNotNull { (key, rowsValue) ->
                val rows = (rowsValue as? List<*>).orEmpty()
                    .mapNotNull { it as? Map<String, Any?> }
                    .map { SmxJsonRow.fromJson(it) }
                    .toMutableList()
                key.toString() to rows
            }?.toMap()?.toMutableMap()

        private fun parseFieldsHeader(value: Any?): MutableMap<String, MutableMap<String, Any?>>? =
            (value as? Map<*, *>)?.mapNotNull { (key, headerValue) ->
                val header = (headerValue as? Map<*, *>)?.mapKeys { it.key.toString() }?.toMutableMap() ?: mutableMapOf()
                key.toString() to header
            }?.toMap()?.toMutableMap()

        private fun parseDate(value: String?): LocalDateTime? {
            if (value.isNullOrBlank()) return null
            return runCatching { LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull()
        }
    }
}
