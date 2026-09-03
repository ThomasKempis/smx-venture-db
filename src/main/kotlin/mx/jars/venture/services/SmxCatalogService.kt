package mx.jars.venture.services

import mx.jars.venture.dataPreview.model.SmxDictionaryObject
import mx.jars.venture.dictionary.SmxDictionaryClient
import mx.jars.venture.dictionary.SmxDictionaryDataSource
import mx.jars.venture.dictionary.SmxDictionaryRowMapper
import mx.jars.venture.model.SmxDatabaseDependency
import mx.jars.venture.model.SmxDatabaseField
import mx.jars.venture.model.SmxDatabaseObject
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet

class SmxCatalogService(
    private val client: SmxDictionaryDataSource = SmxDictionaryClient(),
    private val fieldService: SmxFieldService = SmxFieldService(client),
    private val dependencyService: SmxDependencyService = SmxDependencyService(client),
) {
    fun loadCatalog(): SmxRowSet = client.loadDictionary()

    fun loadObjects(): List<SmxDatabaseObject> =
        SmxDictionaryRowMapper.objects(client.loadDictionary())

    fun loadFields(objectType: String, objectName: String): List<SmxDatabaseField> =
        fieldService.loadFields(objectType, objectName)

    fun loadDependencies(objectType: String, objectName: String): List<SmxDatabaseDependency> =
        dependencyService.loadDependencies(objectType, objectName)

    fun mapCatalog(rowSet: SmxRowSet): List<SmxDatabaseObject> =
        SmxDictionaryRowMapper.objects(rowSet)

    @Deprecated("Use loadFields to keep row-set access out of UI code")
    fun getFields(objectType: String, objectName: String): SmxRowSet =
        client.loadFields(objectType, objectName)

    @Deprecated("Use loadDependencies to keep row-set access out of UI code")
    fun getDependencies(objectType: String, objectName: String): SmxRowSet =
        client.loadDependencies(objectType, objectName)

    fun listObjects(): List<SmxDictionaryObject> =
        loadObjects()
            .filter { it.isSupported() }
            .map(SmxDictionaryObject::from)
}
