package mx.jars.venture.venture_core_connect.smxJson

data class SmxJsonFiles(
    var alias: String? = null,
    var nameFile: String? = null,
    var uuidFile: String? = null,
    var fileBytes: ByteArray? = null,
    var fileUrl: String? = null,
    var accion: String = TipoFileAccion.DOWNLOAD.stringValue,
    var accionView: String = TipoFileAccion.DOWNLOAD_FILE.stringValue,
    var error: String? = null,
    var tipo: String = TipoFileEnum.PNG.stringValue,
    var idSysJson02: Int = -1,
    var servidorUrl: String = "",
    var copyToTemp: Boolean = false,
) {
    fun setList() {
        nameFile = "list"
        accion = TipoFileAccion.LIST.stringValue
    }

    fun toJson(): Map<String, Any?> =
        mapOf(
            "alias" to alias,
            "nameFile" to nameFile,
            "uuidFile" to uuidFile,
            "fileBytes" to fileBytes?.toList(),
            "fileUrl" to fileUrl,
            "accion" to accion,
            "accionView" to accionView,
            "error" to error,
            "tipo" to tipo,
            "idSysJson02" to idSysJson02,
            "servidorUrl" to servidorUrl,
            "copyToTemp" to copyToTemp,
        )

    companion object {
        fun fromJson(json: Map<String, Any?>): SmxJsonFiles {
            val bytes = (json["fileBytes"] as? List<*>)?.mapNotNull { (it as? Number)?.toByte() }?.toByteArray()
            return SmxJsonFiles(
                alias = json["alias"] as? String,
                nameFile = json["nameFile"] as? String,
                uuidFile = json["uuidFile"] as? String,
                fileBytes = bytes,
                fileUrl = json["fileUrl"] as? String,
                accion = json["accion"] as? String ?: TipoFileAccion.DOWNLOAD.stringValue,
                accionView = json["accionView"] as? String ?: TipoFileAccion.DOWNLOAD_FILE.stringValue,
                error = json["error"] as? String,
                tipo = json["tipo"] as? String ?: TipoFileEnum.PNG.stringValue,
                idSysJson02 = (json["idSysJson02"] as? Number)?.toInt() ?: -1,
                servidorUrl = json["servidorUrl"] as? String ?: "",
                copyToTemp = json["copyToTemp"] as? Boolean ?: false,
            )
        }
    }
}
