package mx.jars.venture.model

enum class SmxDependencyKind {
    UNKNOWN,
}

data class SmxDatabaseDependency(
    val target: SmxDatabaseObject,
    val kind: SmxDependencyKind = SmxDependencyKind.UNKNOWN,
)
