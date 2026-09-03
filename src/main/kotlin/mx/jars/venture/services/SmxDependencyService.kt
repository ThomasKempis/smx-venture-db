package mx.jars.venture.services

import mx.jars.venture.dictionary.SmxDictionaryClient
import mx.jars.venture.dictionary.SmxDictionaryDataSource
import mx.jars.venture.dictionary.SmxDictionaryRowMapper
import mx.jars.venture.model.SmxDatabaseDependency

class SmxDependencyService(
    private val client: SmxDictionaryDataSource = SmxDictionaryClient(),
) {
    fun loadDependencies(objectType: String, objectName: String): List<SmxDatabaseDependency> =
        SmxDictionaryRowMapper.dependencies(client.loadDependencies(objectType, objectName))
}
