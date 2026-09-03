package mx.jars.venture.venture_core_connect.smxWebSocketV2

import mx.jars.venture.venture_core_connect.SmxConstantsCore
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private data class PendingMessage(
    val message: String,
    val toBinary: Boolean,
)

object SmxWebSocketCore {
    private val connectTimeout: Duration = Duration.ofSeconds(3)
    private val recoveryDelaySeconds: Long = 2

    private val networkMonitor = SmxNetworkMonitor()
    private val pendingPool: MutableMap<String, PendingMessage> = linkedMapOf()
    private val inFlight: MutableSet<String> = mutableSetOf()
    private val subscribers: MutableMap<String, (String) -> Unit> = mutableMapOf()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "smx-websocket-recovery").apply { isDaemon = true }
    }

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(connectTimeout)
        .build()

    private var activeChannel: WebSocket? = null
    private var guard: SmxPingPongGuard? = null
    private var recoveryTask: CompletableFuture<Void>? = null
    private var recoveryTimer: ScheduledFuture<*>? = null

    private var isOnline: Boolean? = null
    private var channelIsHealthy: Boolean = false
    private var disposed: Boolean = false

    @Synchronized
    fun subscribe(prefix: String, onMessage: (String) -> Unit) {
        subscribers[prefix] = onMessage
    }

    @Synchronized
    fun send(prefix: String, id: String, jsonPayload: String, toBinary: Boolean): CompletableFuture<Void> {
        disposed = false
        pendingPool[id] = PendingMessage("$prefix$jsonPayload", toBinary)
        startNetworkMonitor()

        if (isOnline == true && channelIsHealthy) {
            dispatchPool()
        } else if (isOnline == true) {
            scheduleRecovery()
        }

        return CompletableFuture.completedFuture(null)
    }

    private fun startNetworkMonitor() {
        networkMonitor.start(::handleNetworkStatus)
    }

    @Synchronized
    private fun handleNetworkStatus(online: Boolean) {
        if (disposed || isOnline == online) return
        isOnline = online

        if (!online) {
            recoveryTimer?.cancel(true)
            recoveryTimer = null
            invalidateActiveChannel()
            return
        }

        scheduleRecovery()
    }

    @Synchronized
    private fun scheduleRecovery(delaySeconds: Long = 0) {
        if (disposed ||
            isOnline != true ||
            pendingPool.isEmpty() ||
            channelIsHealthy ||
            recoveryTask != null ||
            recoveryTimer != null
        ) {
            return
        }

        if (delaySeconds > 0) {
            recoveryTimer = scheduler.schedule({
                synchronized(this) {
                    recoveryTimer = null
                }
                startRecovery()
            }, delaySeconds, TimeUnit.SECONDS)
            return
        }

        startRecovery()
    }

    @Synchronized
    private fun startRecovery() {
        if (disposed ||
            isOnline != true ||
            pendingPool.isEmpty() ||
            channelIsHealthy ||
            recoveryTask != null
        ) {
            return
        }

        val task = recoverConnection()
        recoveryTask = task
        task.whenComplete { _, _ ->
            synchronized(this) {
                if (recoveryTask === task) {
                    recoveryTask = null
                }

                if (!disposed && isOnline == true && pendingPool.isNotEmpty() && !channelIsHealthy) {
                    scheduleRecovery(recoveryDelaySeconds)
                }
            }
        }
    }

    private fun recoverConnection(): CompletableFuture<Void> {
        val result = CompletableFuture<Void>()
        val uri = runCatching { URI.create(SmxConstantsCore.strUriSocket) }.getOrNull()
            ?: return result.also { it.complete(null) }

        val listener = SmxWebSocketListener()
        client.newWebSocketBuilder()
            .connectTimeout(connectTimeout)
            .buildAsync(uri, listener)
            .orTimeout(connectTimeout.seconds, TimeUnit.SECONDS)
            .whenComplete { newChannel, error ->
                if (error != null || newChannel == null) {
                    result.complete(null)
                    return@whenComplete
                }

                listener.channel = newChannel

                synchronized(this) {
                    if (disposed || isOnline != true) {
                        closeChannel(newChannel)
                        result.complete(null)
                        return@whenComplete
                    }

                    val oldChannel = activeChannel
                    val newGuard = SmxPingPongGuard(newChannel)
                    activeChannel = newChannel
                    channelIsHealthy = false
                    guard?.dispose()
                    guard = newGuard
                    closeChannel(oldChannel)

                    newGuard.verify().thenAccept { isHealthy ->
                        synchronized(this) {
                            if (activeChannel !== newChannel || guard !== newGuard) {
                                result.complete(null)
                                return@thenAccept
                            }

                            if (!isHealthy || isOnline != true) {
                                invalidateActiveChannel()
                                result.complete(null)
                                return@thenAccept
                            }

                            channelIsHealthy = true
                            inFlight.clear()
                            dispatchPool()
                            result.complete(null)
                        }
                    }
                }
            }

        return result
    }

    @Synchronized
    private fun dispatchPool() {
        val channel = activeChannel
        if (!channelIsHealthy || channel == null) return

        for ((id, pending) in pendingPool.toList()) {
            if (inFlight.contains(id)) continue

            runCatching {
                if (pending.toBinary) {
                    channel.sendBinary(ByteBuffer.wrap(pending.message.toByteArray(StandardCharsets.UTF_8)), true)
                } else {
                    channel.sendText(pending.message, true)
                }
                inFlight.add(id)
            }.onFailure {
                inFlight.remove(id)
                invalidateActiveChannel()
                scheduleRecovery(recoveryDelaySeconds)
                return
            }
        }
    }

    @Synchronized
    private fun handleSocketClose(closedChannel: WebSocket?) {
        if (activeChannel !== closedChannel) return
        invalidateActiveChannel(closeChannel = false)
        scheduleRecovery(recoveryDelaySeconds)
    }

    @Synchronized
    private fun invalidateActiveChannel(closeChannel: Boolean = true) {
        val channel = activeChannel
        activeChannel = null
        channelIsHealthy = false
        inFlight.clear()
        guard?.dispose()
        guard = null

        if (closeChannel) {
            closeChannel(channel)
        }
    }

    private fun closeChannel(channel: WebSocket?) {
        channel?.sendClose(WebSocket.NORMAL_CLOSURE, "closed")
    }

    @Synchronized
    private fun handleRawMessage(source: WebSocket?, message: String) {
        if (activeChannel !== source) return

        if (message == "pong") {
            guard?.handlePong()
            return
        }

        runCatching {
            subscribers.forEach { (prefix, callback) ->
                if (message.startsWith(prefix)) {
                    callback(message.substring(prefix.length))
                }
            }
        }.onFailure {
            println("Error en ruteo core: $it")
        }
    }

    @Synchronized
    fun acknowledge(id: String) {
        pendingPool.remove(id)
        inFlight.remove(id)

        if (pendingPool.isEmpty()) {
            networkMonitor.stopServerChecks()
        }
    }

    @Synchronized
    fun dispose() {
        disposed = true
        recoveryTimer?.cancel(true)
        recoveryTimer = null
        isOnline = null
        invalidateActiveChannel()
        networkMonitor.dispose()
    }

    private class SmxWebSocketListener : WebSocket.Listener {
        var channel: WebSocket? = null
        private val textBuffer = StringBuilder()
        private val binaryBuffer = mutableListOf<Byte>()

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*> {
            textBuffer.append(data)
            if (last) {
                val raw = textBuffer.toString()
                textBuffer.setLength(0)
                handleRawMessage(channel ?: webSocket, raw)
            }
            webSocket.request(1)
            return CompletableFuture.completedFuture(null)
        }

        override fun onBinary(webSocket: WebSocket, data: ByteBuffer, last: Boolean): CompletionStage<*> {
            while (data.hasRemaining()) {
                binaryBuffer.add(data.get())
            }
            if (last) {
                val raw = binaryBuffer.toByteArray().toString(StandardCharsets.UTF_8)
                binaryBuffer.clear()
                handleRawMessage(channel ?: webSocket, raw)
            }
            webSocket.request(1)
            return CompletableFuture.completedFuture(null)
        }

        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*> {
            handleSocketClose(channel ?: webSocket)
            return CompletableFuture.completedFuture(null)
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            handleSocketClose(channel ?: webSocket)
        }
    }
}
