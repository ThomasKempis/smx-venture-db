package mx.jars.venture.venture_core_connect.smxJson

import mx.jars.venture.venture_core_connect.SmxConstantsCore
import mx.jars.venture.venture_core_connect.smxWebSocketV2.smxChannels.SmxWebSocketAppManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

class SmxJsonCall {
    private val MAX_Retries = 5
    private val retryDelayMillis = 4_000L
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun getCallDBServer(
        jsonSend: SmxJsonSend,
        withSocket: Boolean = true,
        withBinary: Boolean = true,
    ): SmxJsonSend =
        if (withSocket) {
            getCallDBServerSocket(jsonSend, withBinary)
        } else {
            getCallDBServerHttp(jsonSend)
        }

    private fun getCallDBServerSocket(jsonSend: SmxJsonSend, withBinary: Boolean): SmxJsonSend {
        jsonSend.error = null
        jsonSend.statuscode = null
        jsonSend.uuidConnect = UUID.randomUUID().toString()

        return SmxWebSocketAppManager.sendApp(jsonSend, withBinary).get(2, TimeUnit.MINUTES)
    }

    private fun getCallDBServerHttp(jsonSend: SmxJsonSend): SmxJsonSend {
        var current = jsonSend
        current.error = null
        current.statuscode = null
        current.uuidConnect = UUID.randomUUID().toString()

        val scheme = if (SmxConstantsCore.urlServidor.contains(":443")) "https" else "http"
        val uri = URI.create("$scheme://${SmxConstantsCore.urlServidor}${SmxConstantsCore.urlWebService}")
        val body = SmxJsonCodec.encode(current.toJson())

        var retryCount = 0
        while (retryCount < MAX_Retries) {
            try {
                val request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()

                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    current = SmxJsonSend.fromJson(SmxJsonCodec.decodeMap(response.body()))
                }

                current.statuscode = response.statusCode().toString()
                break
            } catch (_: Exception) {
                if (retryCount + 1 < MAX_Retries) {
                    current.error = null
                    current.statuscode = null
                    Thread.sleep(retryDelayMillis)
                } else {
                    if (current.error == null) {
                        current.error = "¡Red Inestable!. Verifique su Internet. Reintente en unos segundos."
                    }
                    current.statuscode = "410"
                    break
                }
            }

            retryCount++
        }

        if (current.statuscode == "200") {
            if (current.error != null && current.rowsABM != null) {
                current.rowsABM?.clear()
            }
        } else {
            current.error = current.error ?: "Sin mensaje de error. Operación no realizada"
        }

        return current
    }
}
