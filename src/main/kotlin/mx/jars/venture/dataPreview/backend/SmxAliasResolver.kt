package mx.jars.venture.dataPreview.backend

class SmxAliasResolver {
    fun resolve(objectName: String): String {
        val normalized = objectName.trim().uppercase()
        val parts = normalized.split("_").filter { it.isNotBlank() }

        return when {
            parts.firstOrNull()?.length == ALIAS_LENGTH -> parts.first()
            parts.size > 1 && parts[1].length >= ALIAS_LENGTH -> parts[1].take(ALIAS_LENGTH)
            normalized.length >= ALIAS_LENGTH -> normalized.take(ALIAS_LENGTH)
            else -> normalized
        }
    }

    fun idField(objectName: String): String =
        "ID_${resolve(objectName)}"

    companion object {
        private const val ALIAS_LENGTH = 9
    }
}
