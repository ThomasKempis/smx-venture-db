package mx.jars.venture.dataPreview.backend

import mx.jars.venture.dataPreview.model.SmxDictionaryObject
import mx.jars.venture.dataPreview.model.SmxRelationReference
import mx.jars.venture.dataPreview.model.SmxRelationTarget

class SmxSocSyst10RelationResolver(
    private val objectFinder: SmxDictionaryObjectFinder = SmxDictionaryObjectFinder(),
) {
    fun resolve(
        reference: SmxRelationReference,
        dictionaryObjects: List<SmxDictionaryObject>,
    ): List<SmxRelationTarget>? {
        if (reference.alias != SOCSYST10_ALIAS || !reference.hasSuffix()) return null

        val catalogCode = buildCatalogCode(reference.suffix) ?: return null
        val objectMask = "$CBO_SOCSYST10_PREFIX$catalogCode"
        val targets = objectFinder
            .findByObjectNameMask(objectMask, dictionaryObjects)
            .map { SmxRelationTarget(it.objectType, it.objectName) }

        return targets.ifEmpty {
            listOf(SmxRelationTarget(
            objectType = "VIEW",
            objectName = objectMask,
            ))
        }
    }

    private fun buildCatalogCode(suffix: String): String? {
        val normalized = suffix.trim().uppercase()
        if (normalized.length < 5) return null

        val prefix = normalized.take(3)
        val number = Regex("\\d{2}").find(normalized)?.value ?: return null
        return "$prefix$number"
    }

    companion object {
        private const val SOCSYST10_ALIAS = "SOCSYST10"
        private const val CBO_SOCSYST10_PREFIX = "CBO_SOCSYST10_"
    }
}
