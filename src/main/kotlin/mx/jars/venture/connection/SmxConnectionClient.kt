package mx.jars.venture.connection

import mx.jars.venture.dictionary.SmxDictionaryClient
import mx.jars.venture.venture_core_connect.SmxConstantsCore
import mx.jars.venture.venture_core_connect.smxDataBase.SmxDb
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonCall

class SmxConnectionClient {
    fun connectAndLoadDictionary(
        usuario: String,
        clave: String,
        instancia: String,
        produccion: Boolean = true,
    ): SmxRowSet {
        val sessionProfile = SmxSessionService.configure(usuario, clave, instancia, produccion)
        SmxSessionService.applyToCore(sessionProfile)

        val smxDb = SmxDb { jsonSend, withSocket ->
            SmxJsonCall().getCallDBServer(jsonSend, withSocket = withSocket)
        }

        smxDb.jsonSend.usuario = SmxConstantsCore.usuario
        smxDb.jsonSend.clave = SmxConstantsCore.clave
        smxDb.jsonSend.instancia = SmxConstantsCore.instancia
        smxDb.jsonSend.idEmpresa = SmxConstantsCore.idEmpresa
        smxDb.jsonSend.idUsuario = SmxConstantsCore.idUsuario
        smxDb.jsonSend.idCabOrga = SmxConstantsCore.idCabOrga
        smxDb.jsonSend.idDetDepa = SmxConstantsCore.idDetDepa
        smxDb.jsonSend.urlServidorWeb = SmxConstantsCore.urlServidorWeb
        smxDb.jsonSend.uuidSession = SmxConstantsCore.idSession
        smxDb.jsonSend.regimenFiscal = SmxConstantsCore.regimenFiscal
        smxDb.jsonSend.rfcEmpresa = SmxConstantsCore.rfcEmpresa

        smxDb.addSTrans(
            SmxDictionaryClient.TMPDICC01,
            "SELECT * FROM SP_PLUGIN_DB_CACHE ORDER BY TMPDICC01_ORDEN, TMPDICC01_NOMBRE",
        )

        val response = smxDb.getCallWS(withSocket = true)
        if (response.error != null) {
            SmxSessionService.recordError(response.error)
            throw IllegalStateException(response.error)
        }

        val rowSet = smxDb.getRowset(SmxDictionaryClient.TMPDICC01)
        if (rowSet.getRowCount() == 0) {
            SmxSessionService.recordError("Usuario no reconocido")
            throw IllegalStateException("Usuario no reconocido")
        }

        SmxSessionService.markConnected()
        return rowSet
    }
}
