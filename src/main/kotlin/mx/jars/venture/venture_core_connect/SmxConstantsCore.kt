package mx.jars.venture.venture_core_connect

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class CopyHistory {
    HISTORY_TO_BACKUP,
    BACKUP_TO_HISTORY,
}

object SmxConstantsCore {
    const val rowsXpagina: Int = 50
    const val urlWebService: String = "/sovimex/SMXServletJSonV2"

    var portServidor: Int = 0
    var ipServidor: String = ""
    var urlServidorWeb: String = ""
    var urlServidorSockets: String = ""

    const val socketJson: String = "SMXJSON:"
    const val socketChat: String = "SMXCHAT:"

    val urlServidor: String
        get() = "$ipServidor:$portServidor"

    var instancia: String = ""
    var usuario: String = "INVITADO"
    var clave: String = "INVITADO"
    var idSession: String = UUID.randomUUID().toString()

    var idEmpresa: Int = -1
    var idSicXml01: Int = -1
    var idUsuario: Int = -1
    var usuarioNombres: String = ""
    var usuarioPerfil: String = ""
    var idCabOrga: Int = -1
    var idDetDepa: Int = -1

    var regimenFiscal: String = "601"
    var rfcEmpresa: String = "XAXX010101000"
    var empresa: String = ""
    var departamento: String = ""
    var isReseller: Boolean = false
    var isDesktop: Boolean = false
    var geoLocalizacion: String = ""

    var testCfdi: Boolean = true
    var saldoFolios: Int = 0

    var idFalse: Int = -10000

    private val rowHistoryTrans: MutableMap<String, MutableMap<Int, Any>> = mutableMapOf()
    private val rowHistoryTransBackup: MutableMap<String, MutableMap<Int, Any>> = mutableMapOf()

    val sqlsInit: MutableMap<String, String> = mutableMapOf()

    val strUriSocket: String
        get() = getUriSocket(instancia = instancia, usuario = usuario)

    fun getUriSocket(instancia: String, usuario: String): String {
        val instanciaEncoded = URLEncoder.encode(instancia, StandardCharsets.UTF_8)
        val usuarioEncoded = URLEncoder.encode(usuario, StandardCharsets.UTF_8)
        return "${urlServidorSockets}instancia=$instanciaEncoded&usuario=$usuarioEncoded"
    }

    fun initSetupConnection(produccion: Boolean) {
        if (produccion) {
            portServidor = 443
            ipServidor = "venture.jars.mx"
            urlServidorWeb = "https://$urlServidor"
            instancia = "VENTURE"
            urlServidorSockets = "wss://$ipServidor:8443/?"
        } else {
            portServidor = 8080
            ipServidor = "192.168.148.132"
            urlServidorWeb = "http://$urlServidor"
            instancia = "SMXSIC"
            urlServidorSockets = "ws://$ipServidor:8082/?"
        }
    }

    fun setRowHistory(alias: String, id: Int, rowset: Any) {
        val key = alias.uppercase()
        rowHistoryTrans.getOrPut(key) { mutableMapOf() }[id] = rowset
        rowHistoryTransBackup.getOrPut(key) { mutableMapOf() }[id] = rowset
    }

    fun isRowHistory(alias: String, id: Int): Boolean =
        rowHistoryTrans[alias.uppercase()]?.containsKey(id) == true

    fun clearRowHistory() {
        rowHistoryTrans.clear()
        rowHistoryTransBackup.clear()
    }

    fun removeRowHistory(alias: String, id: Int) {
        rowHistoryTrans[alias.uppercase()]?.remove(id)
        rowHistoryTransBackup[alias.uppercase()]?.remove(id)
    }

    fun getRowHistory(alias: String, id: Int): Any? =
        rowHistoryTrans[alias.uppercase()]?.get(id)

    fun copyRowHistory(alias: String, modalidad: CopyHistory) {
        val key = alias.uppercase()
        if (key.isBlank()) return

        when (modalidad) {
            CopyHistory.HISTORY_TO_BACKUP -> rowHistoryTrans[key]?.let {
                rowHistoryTransBackup[key] = it.toMutableMap()
            }

            CopyHistory.BACKUP_TO_HISTORY -> rowHistoryTransBackup[key]?.let {
                rowHistoryTrans[key] = it.toMutableMap()
            }
        }
    }
}
