package mx.jars.venture.dataPreview.backend

class SmxIdFieldResolver(
    private val aliasResolver: SmxAliasResolver = SmxAliasResolver(),
) {
    fun resolveFromObject(objectType: String, objectName: String): String? =
        resolveFromAlias(objectType, aliasResolver.resolve(objectName))

    fun resolveFromFields(objectType: String, objectName: String, fieldNames: Collection<String>): String? {
        val normalizedType = objectType.trim().uppercase()
        if (normalizedType == "PROCEDURE") return null

        val normalizedFields = fieldNames
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }

        val expectedId = resolveFromObject(objectType, objectName)
        return normalizedFields.firstOrNull { it == expectedId }
            ?: normalizedFields.firstOrNull { it.startsWith("ID_") }
    }

    fun resolveFromAlias(objectType: String, alias: String): String? {
        val normalizedType = objectType.trim().uppercase()
        val normalizedAlias = alias.trim().uppercase()
        if (normalizedAlias.isBlank()) return null

        return when (normalizedType) {
            "TABLE", "VIEW" -> "ID_$normalizedAlias"
            "PROCEDURE" -> null
            else -> null
        }
    }
}
