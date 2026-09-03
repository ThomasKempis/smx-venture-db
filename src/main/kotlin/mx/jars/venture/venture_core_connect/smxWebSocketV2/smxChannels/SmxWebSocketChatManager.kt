package mx.jars.venture.venture_core_connect.smxWebSocketV2.smxChannels

import mx.jars.venture.venture_core_connect.smxJson.SmxJsonChat
import mx.jars.venture.venture_core_connect.smxJson.SmxJsonCodec
import mx.jars.venture.venture_core_connect.smxWebSocketV2.SmxWebSocketCore
import java.time.ZoneOffset
import java.util.concurrent.CompletableFuture

object SmxWebSocketChatManager {
    private const val prefix = "SMXCHAT:"

    private var onChatReceived: ((SmxJsonChat) -> Unit)? = null

    init {
        SmxWebSocketCore.subscribe(prefix) { payload ->
            runCatching {
                val response = SmxJsonChat.fromJson(SmxJsonCodec.decodeMap(payload))
                onChatReceived?.invoke(response)

                val millis = response.fecha.toInstant(ZoneOffset.UTC).toEpochMilli()
                val poolId = response.jsonSend?.uuidConnect ?: "CHAT_${response.correo}_$millis"
                SmxWebSocketCore.acknowledge(poolId)
            }.onFailure {
                println("ChatManager Error: $it")
            }
        }
    }

    fun setCallback(callback: (SmxJsonChat) -> Unit) {
        onChatReceived = callback
    }

    fun send(chat: SmxJsonChat, toBinary: Boolean = true): CompletableFuture<Void> {
        val millis = chat.fecha.toInstant(ZoneOffset.UTC).toEpochMilli()
        val poolId = chat.jsonSend?.uuidConnect ?: "CHAT_${chat.correo}_$millis"

        return SmxWebSocketCore.send(
            prefix = prefix,
            id = poolId,
            jsonPayload = SmxJsonCodec.encode(chat.toJson()),
            toBinary = toBinary,
        )
    }

    fun dispose() {
        onChatReceived = null
    }
}
