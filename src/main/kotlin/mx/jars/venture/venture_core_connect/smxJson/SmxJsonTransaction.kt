package mx.jars.venture.venture_core_connect.smxJson

data class SmxJsonTransaction(
    var rowCabTransaction: SmxJsonRow? = null,
) {
    fun smxJsonSendTransaction(rowCabTransaction: SmxJsonRow) {
        this.rowCabTransaction = rowCabTransaction
    }

    fun toJson(): Map<String, Any?> =
        mapOf("rowCabTransaction" to rowCabTransaction?.toJson())

    fun clone(): SmxJsonTransaction =
        SmxJsonTransaction(rowCabTransaction = rowCabTransaction?.clone())

    companion object {
        fun fromJson(json: Map<String, Any?>): SmxJsonTransaction =
            SmxJsonTransaction(
                rowCabTransaction = (json["rowCabTransaction"] as? Map<String, Any?>)?.let { SmxJsonRow.fromJson(it) },
            )
    }
}
