package mx.jars.venture.services

import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta

data class SmxObjectMetadata(
    val objectType: String,
    val objectName: String,
    val fields: Map<String, SmxDataPreviewFieldMeta>,
) {
    val parameterFields: List<SmxDataPreviewFieldMeta>
        get() = fields.values.filter { it.isParameter() }

    val outputFields: List<SmxDataPreviewFieldMeta>
        get() = fields.values.filter { !it.isParameter() }
}
