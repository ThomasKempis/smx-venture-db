package mx.jars.venture.dictionary

import mx.jars.venture.connection.SmxSessionService
import mx.jars.venture.venture_core_connect.SmxConstantsCore
import mx.jars.venture.venture_core_connect.smxDataBase.SmxDb
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonCall

class SmxDictionaryClient : SmxDictionaryDataSource {
    override fun loadDictionary(): SmxRowSet {
        val smxDb = buildDb()

        smxDb.addSTrans(
            TMPDICC01,
            "SELECT * FROM SP_PLUGIN_DB_CACHE ORDER BY TMPDICC01_ORDEN, TMPDICC01_NOMBRE",
        )
        smxDb.getCallWS(withSocket = true)
        return smxDb.getRowset(TMPDICC01)
    }

    override fun loadFields(tipoObjeto: String, objeto: String): SmxRowSet {
        val smxDb = buildDb()
        val tipoSql = sql(tipoObjeto.uppercase())
        val objetoSql = sql(objeto.uppercase())

        smxDb.addSTrans(
            TMPFLD01,
            "SELECT * FROM SP_PLUGIN_FIELDS('$tipoSql', '$objetoSql') " +
                "ORDER BY TMPFLD01_SECCION, TMPFLD01_POSICION",
        )
        smxDb.getCallWS(withSocket = true)
        return smxDb.getRowset(TMPFLD01)
    }

    override fun loadDependencies(tipoObjeto: String, objeto: String): SmxRowSet {
        val smxDb = buildDb()
        val tipoSql = sql(tipoObjeto.uppercase())
        val objetoSql = sql(objeto.uppercase())

        smxDb.addSTrans(
            TMPDEP01,
            "SELECT * FROM SP_PLUGIN_DEPENDENCIES('$tipoSql', '$objetoSql') " +
                "ORDER BY TMPDICC01_ORDEN, TMPDICC01_NOMBRE",
        )
        smxDb.getCallWS(withSocket = true)
        return smxDb.getRowset(TMPDEP01)
    }

    private fun buildDb(): SmxDb {
        val smxDb = SmxDb { jsonSend, withSocket ->
            SmxJsonCall().getCallDBServer(jsonSend, withSocket = withSocket)
        }

        val profile = SmxSessionService.currentProfile()

        smxDb.jsonSend.usuario = profile.usuario.ifBlank { SmxConstantsCore.usuario }
        smxDb.jsonSend.clave = profile.clave.ifBlank { SmxConstantsCore.clave }
        smxDb.jsonSend.instancia = profile.instancia.ifBlank { SmxConstantsCore.instancia }
        smxDb.jsonSend.idEmpresa = SmxConstantsCore.idEmpresa
        smxDb.jsonSend.idUsuario = SmxConstantsCore.idUsuario
        smxDb.jsonSend.idCabOrga = SmxConstantsCore.idCabOrga
        smxDb.jsonSend.idDetDepa = SmxConstantsCore.idDetDepa
        smxDb.jsonSend.urlServidorWeb = profile.urlServidorWeb.ifBlank { SmxConstantsCore.urlServidorWeb }
        smxDb.jsonSend.uuidSession = profile.idSession.ifBlank { SmxConstantsCore.idSession }
        smxDb.jsonSend.regimenFiscal = SmxConstantsCore.regimenFiscal
        smxDb.jsonSend.rfcEmpresa = SmxConstantsCore.rfcEmpresa

        return smxDb
    }

    private fun sql(value: String): String =
        value.trim().replace("'", "''")

    companion object {
        const val TMPDICC01 = "TMPDICC01"
        const val TMPFLD01 = "TMPFLD01"
        const val TMPDEP01 = "TMPDEP01"
    }
}
