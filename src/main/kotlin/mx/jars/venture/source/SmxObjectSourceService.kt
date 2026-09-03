package mx.jars.venture.source

import mx.jars.venture.connection.SmxSessionService
import mx.jars.venture.venture_core_connect.SmxConstantsCore
import mx.jars.venture.venture_core_connect.smxDataBase.SmxDb
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonCall

class SmxObjectSourceService {
    fun loadSource(objectType: String, objectName: String): String {
        val normalizedType = objectType.trim().uppercase()
        val normalizedName = objectName.trim().ifBlank { return "" }

        if (normalizedType !in setOf("VIEW", "PROCEDURE")) {
            return ""
        }

        val query = when (normalizedType) {
            "VIEW" -> "SELECT 0 AS ID_SOURCE, RDB\$VIEW_SOURCE AS SOURCE FROM RDB\$RELATIONS WHERE UPPER(RDB\$RELATION_NAME) = '${normalizedName.uppercase()}'"
            "PROCEDURE" -> "SELECT 0 AS ID_SOURCE, RDB\$PROCEDURE_SOURCE AS SOURCE FROM RDB\$PROCEDURES WHERE UPPER(RDB\$PROCEDURE_NAME) = '${normalizedName.uppercase()}'"
            else -> return ""
        }

        val db = SmxDb { jsonSend, withSocket ->
            SmxJsonCall().getCallDBServer(jsonSend, withSocket = withSocket)
        }

        val profile = SmxSessionService.currentProfile()
        db.jsonSend.usuario = profile.usuario.ifBlank { SmxConstantsCore.usuario }
        db.jsonSend.clave = profile.clave.ifBlank { SmxConstantsCore.clave }
        db.jsonSend.instancia = profile.instancia.ifBlank { SmxConstantsCore.instancia }
        db.jsonSend.idEmpresa = SmxConstantsCore.idEmpresa
        db.jsonSend.idUsuario = SmxConstantsCore.idUsuario
        db.jsonSend.idCabOrga = SmxConstantsCore.idCabOrga
        db.jsonSend.idDetDepa = SmxConstantsCore.idDetDepa
        db.jsonSend.urlServidorWeb = profile.urlServidorWeb.ifBlank { SmxConstantsCore.urlServidorWeb }
        db.jsonSend.uuidSession = profile.idSession.ifBlank { SmxConstantsCore.idSession }
        db.jsonSend.regimenFiscal = SmxConstantsCore.regimenFiscal
        db.jsonSend.rfcEmpresa = SmxConstantsCore.rfcEmpresa

        db.addSql("SOURCE", query)
        val response = db.callSql(withSocket = true)
        if (response.error != null) {
            SmxSessionService.recordError(response.error)
            throw IllegalStateException(response.error)
        }

        val rowSet = db.getRowset("SOURCE")
        if (rowSet.getRowCount() == 0) {
            return ""
        }

        rowSet.beforeFirst()
        if (!rowSet.next()) {
            return ""
        }

        val source = rowSet.getValue("SOURCE")?.toString().orEmpty()
        return if (normalizedType == "VIEW") {
            source.replaceFirst(Regex("^(?:[\\t ]*(?:\\r\\n|\\n|\\r))+"), "")
        } else {
            source
        }
    }
}
