package mx.jars.venture.services

import mx.jars.venture.dataPreview.SmxDataPreviewRequest
import mx.jars.venture.dataPreview.backend.SmxDataPreviewClient
import mx.jars.venture.dataPreview.model.SmxDataPreviewFieldMeta
import mx.jars.venture.dataPreview.model.SmxDataPreviewFilter
import mx.jars.venture.dataPreview.model.SmxDataPreviewSortDirection
import mx.jars.venture.dataPreview.model.SmxDictionaryObject
import mx.jars.venture.venture_core_connect.smxDataBase.SmxRowSet

data class SmxPreviewQueryOptions(
    val orderField: String? = null,
    val orderDirection: SmxDataPreviewSortDirection = SmxDataPreviewSortDirection.ASC,
    val page: Int = 1,
    val procedureParameters: Map<String, String> = emptyMap(),
    val fieldMeta: Map<String, SmxDataPreviewFieldMeta> = emptyMap(),
    val filters: List<SmxDataPreviewFilter> = emptyList(),
)

class SmxPreviewService(
    private val client: SmxDataPreviewClient = SmxDataPreviewClient(),
    private val fieldService: SmxFieldService = SmxFieldService(),
    private val catalogService: SmxCatalogService = SmxCatalogService(),
) {
    fun loadPage(
        request: SmxDataPreviewRequest,
        options: SmxPreviewQueryOptions = SmxPreviewQueryOptions(),
    ): SmxRowSet = client.loadPage(
        objectType = request.objectType,
        objectName = request.objectName,
        orderField = options.orderField,
        orderDirection = options.orderDirection,
        pageSize = request.pageSize,
        page = options.page,
        procedureParameters = options.procedureParameters,
        fieldMeta = options.fieldMeta,
        filters = options.filters,
    )

    fun loadMetadata(request: SmxDataPreviewRequest): SmxObjectMetadata {
        val fields = fieldService.loadPreviewMetadata(request.objectType, request.objectName)
        return SmxObjectMetadata(
            objectType = request.objectType,
            objectName = request.objectName,
            fields = fields,
        )
    }

    fun loadFieldMeta(objectType: String, objectName: String): Map<String, SmxDataPreviewFieldMeta> =
        fieldService.loadPreviewMetadata(objectType, objectName)

    fun listObjects(): List<SmxDictionaryObject> =
        catalogService.loadObjects()
            .filter { it.isSupported() }
            .map(SmxDictionaryObject::from)
}
