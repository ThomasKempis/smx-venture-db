package mx.jars.venture.model

enum class SmxDatabaseFieldSection {
    FIELD("CAMPO"),
    PARAMETER("PARAMETRO"),
    OUTPUT("SALIDA"),
    OTHER("OTRO"),
    ;

    val apiName: String

    constructor(apiName: String) {
        this.apiName = apiName
    }

    companion object {
        fun from(value: String?): SmxDatabaseFieldSection =
            when (value?.trim()?.uppercase()) {
                "PARAMETRO", "PARAMETER" -> PARAMETER
                "SALIDA", "OUTPUT" -> OUTPUT
                "CAMPO", "FIELD" -> FIELD
                else -> OTHER
            }
    }
}

data class SmxDatabaseField(
    val name: String,
    val dataType: String,
    val section: SmxDatabaseFieldSection = SmxDatabaseFieldSection.FIELD,
    val position: Int = 0,
    val length: Int = 0,
    val scale: Int = 0,
    val precision: Int = 0,
    val required: Boolean = false,
) {
    val formattedType: String
        get() = if (dataType.equals("VARCHAR", ignoreCase = true) && length > 0) {
            "VARCHAR($length)"
        } else {
            dataType
        }

    fun isParameter(): Boolean = section == SmxDatabaseFieldSection.PARAMETER

    fun isOutput(): Boolean = section == SmxDatabaseFieldSection.OUTPUT
}
