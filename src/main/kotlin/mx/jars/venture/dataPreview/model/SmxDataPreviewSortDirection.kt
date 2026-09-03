package mx.jars.venture.dataPreview.model

enum class SmxDataPreviewSortDirection(
    val sql: String,
    val marker: String,
) {
    ASC("ASC", " ▲"),
    DESC("DESC", " ▼");

    fun toggle(): SmxDataPreviewSortDirection =
        if (this == ASC) DESC else ASC
}
