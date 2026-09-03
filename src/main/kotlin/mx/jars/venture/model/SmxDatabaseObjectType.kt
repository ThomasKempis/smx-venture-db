package mx.jars.venture.model

enum class SmxDatabaseObjectType(
    val apiName: String,
) {
    TABLE("TABLE"),
    VIEW("VIEW"),
    PROCEDURE("PROCEDURE"),
    UNKNOWN("UNKNOWN"),
    ;

    companion object {
        fun from(value: String?): SmxDatabaseObjectType {
            val normalized = value?.trim()?.uppercase().orEmpty()
            return when {
                normalized == "TABLE" || normalized == "TABLA" || normalized == "T" -> TABLE
                normalized == "VIEW" || normalized == "VISTA" || normalized == "V" -> VIEW
                normalized == "PROCEDURE" || normalized == "PROCEDIMIENTO" ||
                    normalized == "P" || normalized == "SP" -> PROCEDURE
                normalized.contains("TABLE") || normalized.contains("TABLA") -> TABLE
                normalized.contains("VIEW") || normalized.contains("VISTA") -> VIEW
                normalized.contains("PROCED") -> PROCEDURE
                else -> UNKNOWN
            }
        }
    }
}
