package mx.jars.venture.dataPreview

import mx.jars.venture.venture_core_connect.SmxConstantsCore

data class SmxDataPreviewRequest(
    val objectType: String,
    val objectName: String,
    val pageSize: Int = SmxConstantsCore.rowsXpagina,
)
