package mx.jars.venture.dataPreview.backend

import mx.jars.venture.dataPreview.model.SmxDictionaryObject

class SmxDictionaryObjectFinder(
    private val aliasResolver: SmxAliasResolver = SmxAliasResolver(),
) {
    fun findByAlias(alias: String, dictionaryObjects: List<SmxDictionaryObject>): SmxDictionaryObject? {
        val normalizedAlias = alias.trim().uppercase()
        if (normalizedAlias.isBlank()) return null

        return dictionaryObjects
            .filter { aliasResolver.resolve(it.objectName) == normalizedAlias }
            .sortedWith(compareBy<SmxDictionaryObject> { objectTypeWeight(it.objectType) }
                .thenBy { objectNameWeight(it.objectName, normalizedAlias) }
                .thenBy { it.objectName })
            .firstOrNull()
    }

    fun findByObjectNameMask(mask: String, dictionaryObjects: List<SmxDictionaryObject>): List<SmxDictionaryObject> {
        val normalizedMask = mask.trim().uppercase()
        if (normalizedMask.isBlank()) return emptyList()

        return dictionaryObjects
            .filter { it.objectName.startsWith(normalizedMask) }
            .distinctBy { "${it.objectType}|${it.objectName}" }
            .sortedWith(compareBy<SmxDictionaryObject> { if (it.objectName == normalizedMask) 0 else 1 }
                .thenBy { objectTypeWeight(it.objectType) }
                .thenBy { it.objectName })
    }

    private fun objectTypeWeight(objectType: String): Int =
        when (objectType.uppercase()) {
            "TABLE" -> 0
            "VIEW" -> 1
            "PROCEDURE" -> 2
            else -> 3
        }

    private fun objectNameWeight(objectName: String, alias: String): Int =
        when {
            objectName == alias -> 0
            objectName.startsWith("${alias}_CAB_") -> 1
            objectName.startsWith("${alias}_DET_") -> 2
            objectName.startsWith(alias) -> 3
            else -> 4
        }
}
