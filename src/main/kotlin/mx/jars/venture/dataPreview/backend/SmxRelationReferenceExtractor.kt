package mx.jars.venture.dataPreview.backend

import mx.jars.venture.dataPreview.model.SmxRelationReference

class SmxRelationReferenceExtractor {
    fun extract(fieldName: String): SmxRelationReference? {
        val normalized = fieldName.trim().uppercase()
        if (!normalized.startsWith(RELA_PREFIX)) return null

        val payload = normalized.removePrefix(RELA_PREFIX)
        val alias = payload.take(ALIAS_LENGTH).trim('_')
        if (alias.isBlank()) return null

        val suffix = payload
            .drop(alias.length)
            .trimStart('_')

        return SmxRelationReference(alias, suffix)
    }

    companion object {
        private const val RELA_PREFIX = "RELA_"
        private const val ALIAS_LENGTH = 9
    }
}
