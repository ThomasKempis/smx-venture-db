package mx.jars.venture.dataPreview.model

import mx.jars.venture.model.SmxDatabaseObject
import mx.jars.venture.model.SmxDatabaseObjectType

/**
 * Compatibility DTO for preview resolvers. New catalog code uses [SmxDatabaseObject].
 */
data class SmxDictionaryObject(
    val objectType: String,
    val objectName: String,
) {
    fun toDatabaseObject(): SmxDatabaseObject =
        SmxDatabaseObject(
            type = SmxDatabaseObjectType.from(objectType),
            name = objectName,
        )

    companion object {
        fun from(databaseObject: SmxDatabaseObject): SmxDictionaryObject =
            SmxDictionaryObject(databaseObject.type.apiName, databaseObject.normalizedName)
    }
}
