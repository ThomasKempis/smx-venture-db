package mx.jars.venture.venture_core_connect.smxWebSocketV2.smxChannels

import mx.jars.venture.venture_core_connect.smxJson.SmxJsonCodec
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonSend
import mx.jars.venture.venture_core_connect.smxWebSocketV2.SmxWebSocketCore
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object SmxWebSocketAppManager {
    private const val prefix = "SMXAPP:"
    private const val uiTimeoutMinutes: Long = 2

    private val waitingResponses: MutableMap<String, CompletableFuture<SmxJsonSend>> = mutableMapOf()

    init {
        SmxWebSocketCore.subscribe(prefix) { payload ->
            runCatching {
                val response = SmxJsonSend.fromJson(SmxJsonCodec.decodeMap(payload))
                val uuid = response.uuidConnect

                waitingResponses.remove(uuid)?.complete(response)
                SmxWebSocketCore.acknowledge(uuid)
            }.onFailure {
                println("AppManager Error: $it")
            }
        }
    }

    fun sendApp(jsonSend: SmxJsonSend, toBinary: Boolean): CompletableFuture<SmxJsonSend> {
        val uuid = jsonSend.uuidConnect
        val waiting = CompletableFuture<SmxJsonSend>()
        waitingResponses[uuid] = waiting

        SmxWebSocketCore.send(
            prefix = prefix,
            id = uuid,
            jsonPayload = SmxJsonCodec.encode(jsonSend.toJson()),
            toBinary = toBinary,
        )

        return waiting.completeOnTimeout(
            jsonSend.copy().also { it.error = "Tiempo de espera agotado (Timeout)" },
            uiTimeoutMinutes,
            TimeUnit.MINUTES,
        ).whenComplete { _, _ ->
            waitingResponses.remove(uuid)
            SmxWebSocketCore.acknowledge(uuid)
        }
    }

    fun dispose() {
        waitingResponses.clear()
    }
}
