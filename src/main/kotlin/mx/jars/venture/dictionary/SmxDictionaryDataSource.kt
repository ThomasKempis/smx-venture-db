package mx.jars.venture.dictionary

import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet

interface SmxDictionaryDataSource {
    fun loadDictionary(): SmxRowSet

    fun loadFields(tipoObjeto: String, objeto: String): SmxRowSet

    fun loadDependencies(tipoObjeto: String, objeto: String): SmxRowSet
}
