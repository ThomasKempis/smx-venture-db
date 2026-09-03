package mx.jars.venture.dictionary

import mx.jars.venture.model.SmxDatabaseDependency
import mx.jars.venture.model.SmxDatabaseField
import mx.jars.venture.model.SmxDatabaseFieldSection
import mx.jars.venture.model.SmxDatabaseObject
import mx.jars.venture.model.SmxDatabaseObjectType
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet

object SmxDictionaryRowMapper {
    fun objects(rowSet: SmxRowSet): List<SmxDatabaseObject> {
        val objects = mutableListOf<SmxDatabaseObject>()
        rowSet.beforeFirst()
        while (rowSet.next()) {
            val nodeId = value(rowSet, "TMPDICC01_NODE_ID")
            val parentId = value(rowSet, "TMPDICC01_PARENT_ID")
            val displayName = value(rowSet, "TMPDICC01_NOMBRE")
            val type = SmxDatabaseObjectType.from(value(rowSet, "TMPDICC01_TIPO"))
            val name = value(rowSet, "TMPDICC01_OBJETO").ifBlank { displayName }
            if (nodeId.isBlank() || displayName.isBlank()) continue

            objects += SmxDatabaseObject(
                type = type,
                name = name.trim(),
                displayName = displayName.trim(),
                nodeId = nodeId.trim(),
                parentId = parentId.trim(),
                order = number(rowSet, "TMPDICC01_ORDEN"),
            )
        }
        return objects
    }

    fun fields(rowSet: SmxRowSet): List<SmxDatabaseField> {
        val fields = mutableListOf<SmxDatabaseField>()
        rowSet.beforeFirst()
        while (rowSet.next()) {
            val name = value(rowSet, "TMPFLD01_CAMPO")
            if (name.isBlank()) continue

            fields += SmxDatabaseField(
                name = name.trim(),
                dataType = value(rowSet, "TMPFLD01_TIPO_DATO"),
                section = SmxDatabaseFieldSection.from(value(rowSet, "TMPFLD01_SECCION")),
                position = number(rowSet, "TMPFLD01_POSICION"),
                length = number(rowSet, "TMPFLD01_LONGITUD"),
                scale = number(rowSet, "TMPFLD01_ESCALA"),
                precision = number(rowSet, "TMPFLD01_PRECISION"),
                required = isTrue(rowSet.getValue("TMPFLD01_REQUERIDO")),
            )
        }
        return fields
    }

    fun dependencies(rowSet: SmxRowSet): List<SmxDatabaseDependency> =
        objects(rowSet)
            .filter { !it.isRoot && it.isSupported() }
            .map(::SmxDatabaseDependency)

    private fun value(rowSet: SmxRowSet, key: String): String =
        rowSet.getValue(key)?.toString()?.trim().orEmpty()

    private fun number(rowSet: SmxRowSet, key: String): Int =
        (rowSet.getValue(key) as? Number)?.toInt()
            ?: value(rowSet, key).toIntOrNull()
            ?: 0

    private fun isTrue(value: Any?): Boolean =
        value?.toString()?.trim()?.uppercase() in setOf("S", "SI", "Y", "YES", "1", "TRUE")
}
