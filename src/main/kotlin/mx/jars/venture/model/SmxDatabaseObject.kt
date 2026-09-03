package mx.jars.venture.model

data class SmxDatabaseObject(
    val type: SmxDatabaseObjectType,
    val name: String,
    val displayName: String = name,
    val nodeId: String = name,
    val parentId: String = "",
    val order: Int = 0,
) {
    val isRoot: Boolean
        get() = parentId.isBlank()

    val normalizedName: String
        get() = name.trim().uppercase()

    fun isSupported(): Boolean = type != SmxDatabaseObjectType.UNKNOWN
}
