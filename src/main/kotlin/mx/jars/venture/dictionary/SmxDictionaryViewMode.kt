package mx.jars.venture.dictionary

import mx.jars.venture.model.SmxDatabaseObject
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet

data class SmxDictionaryViewMode(
    val label: String,
    val objects: List<SmxDatabaseObject>?,
    @Deprecated("Use objects")
    val rowSet: SmxRowSet? = null,
) {
    constructor(label: String, rowSet: SmxRowSet?) : this(
        label = label,
        objects = rowSet?.let(SmxDictionaryRowMapper::objects),
        rowSet = rowSet,
    )

    override fun toString(): String = label

    companion object {
        val ALL = SmxDictionaryViewMode(label = "Todos", objects = null)
    }
}
