package mx.jars.venture.dataPreview.model

object SmxFieldOrdering {
    fun <T> sort(fields: Collection<T>, name: (T) -> String): List<T> =
        fields.sortedWith(compareBy { priority(name(it)) })

    fun priority(fieldName: String): Int {
        val normalized = fieldName.uppercase()
        return when {
            normalized.startsWith("RELA_") -> 0
            normalized.startsWith("ID_") -> 1
            normalized.endsWith("_FAPL") -> 3
            else -> 2
        }
    }
}
