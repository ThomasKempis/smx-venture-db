package mx.jars.venture.services

import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta
import mx.jars.venture.dictionary.SmxDictionaryClient
import mx.jars.venture.dictionary.SmxDictionaryDataSource
import mx.jars.venture.dictionary.SmxDictionaryRowMapper
import mx.jars.venture.model.SmxDatabaseField

class SmxFieldService(
    private val client: SmxDictionaryDataSource = SmxDictionaryClient(),
) {
    fun loadFields(objectType: String, objectName: String): List<SmxDatabaseField> =
        SmxDictionaryRowMapper.fields(client.loadFields(objectType, objectName))

    fun loadPreviewMetadata(objectType: String, objectName: String): Map<String, SmxDataPreviewFieldMeta> =
        loadFields(objectType, objectName)
            .associate { field ->
                field.name.uppercase() to SmxDataPreviewFieldMeta(
                    name = field.name,
                    dataType = field.dataType,
                    section = field.section.apiName,
                    length = field.length,
                )
            }
}
