package mx.jars.venture.dataPreview.model

data class SmxRelationReference(
    val alias: String,
    val suffix: String,
) {
    fun hasSuffix(): Boolean = suffix.isNotBlank()
}
