package mx.jars.venture.venture_core_connect.smxJson

enum class TipoField {
    NON,
    DAT,
    INT,
    DBL,
    LNG,
    STR,
    LOG,
    BIN;

    fun toJson(): String = name

    companion object {
        fun fromString(value: String?): TipoField =
            entries.firstOrNull { it.name == value } ?: NON
    }
}

enum class AccionNN {
    R,
    W,
    D,
    I,
    N;

    fun toJson(): String = name

    companion object {
        fun fromString(value: String?): AccionNN =
            entries.firstOrNull { it.name == value } ?: N
    }
}

enum class TipoFileAccion {
    UPLOAD,
    DOWNLOAD,
    DOWNLOAD_URL,
    DOWNLOAD_FILE,
    LIST;

    val stringValue: String
        get() = name
}

enum class TipoFileEnum {
    PNG,
    JPG,
    JPEG,
    PDF,
    CER,
    KEY,
    NON;

    val stringValue: String
        get() = name
}

enum class SmxServicios {
    CRUD_SQL,
    CRUD_UPDATE,
    CRUD_DELETE,
    CRUD_INSERT,
    CRUD_TRANS,
    FILES,
    MAIL,
    LOGON,
    LOGON_REGISTRO,
    LOGON_UUID,
    CFDI,
    SOCMAIN01_SOCUSUA01,
    EXPORT_EXCEL,
    REPORTS,
    VID_PDFS_PRODUCCION,
    CMO_XLSX_MIGRACION;

    fun toStringValue(): String = name
}

enum class CallWsResponse {
    SETUP,
    OK,
    ERROR,
}
