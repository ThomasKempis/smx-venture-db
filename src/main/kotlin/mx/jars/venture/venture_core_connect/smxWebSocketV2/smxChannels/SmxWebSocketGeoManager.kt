package mx.jars.venture.venture_core_connect.smxWebSocketV2.smxChannels

import mx.jars.venture.venture_core_connect.smxJson.SmxJsonCodec
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonGeo
import mx.jars.venture.venture_core_connect.smxWebSocketV2.SmxWebSocketCore
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture

object SmxWebSocketGeoManager {
    private const val prefix = "SMXGEO:"

    private var onGeoReceived: ((SmxJsonGeo) -> Unit)? = null

    init {
        SmxWebSocketCore.subscribe(prefix) { payload ->
            runCatching {
                val response = SmxJsonGeo.fromJson(SmxJsonCodec.decodeMap(payload))
                onGeoReceived?.invoke(response)

                val poolId = response.jsonSend?.uuidConnect ?: "GEO_${response.correo}"
                SmxWebSocketCore.acknowledge(poolId)
            }.onFailure {
                println("GeoManager Error: $it")
            }
        }
    }

    fun setCallback(callback: (SmxJsonGeo) -> Unit) {
        onGeoReceived = callback
    }

    fun send(geo: SmxJsonGeo, toBinary: Boolean): CompletableFuture<Void> {
        val poolId = geo.jsonSend?.uuidConnect ?: "GEO_${geo.correo}"
        return SmxWebSocketCore.send(
            prefix = prefix,
            id = poolId,
            jsonPayload = SmxJsonCodec.encode(geo.toJson()),
            toBinary = toBinary,
        )
    }

    fun dispose() {
        onGeoReceived = null
    }
}
