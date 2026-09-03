package mx.jars.venture.venture_core_connect.smxWebSocketV2

import java.net.http.WebSocket
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SmxPingPongGuard(
    private val channel: WebSocket,
) {
    private val pongTimeoutSeconds: Long = 3
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "smx-ping-pong").apply { isDaemon = true }
    }

    private var pendingVerification: CompletableFuture<Boolean>? = null
    private var timeout: ScheduledFuture<*>? = null

    @Synchronized
    fun verify(): CompletableFuture<Boolean> {
        pendingVerification?.let { return it }

        val verification = CompletableFuture<Boolean>()
        pendingVerification = verification
        timeout = scheduler.schedule({ complete(false) }, pongTimeoutSeconds, TimeUnit.SECONDS)

        runCatching {
            channel.sendText("ping", true)
        }.onFailure {
            complete(false)
        }

        verification.whenComplete { _, _ ->
            synchronized(this) {
                timeout?.cancel(true)
                timeout = null
                pendingVerification = null
            }
        }

        return verification
    }

    fun handlePong() {
        complete(true)
    }

    fun dispose() {
        complete(false)
        scheduler.shutdownNow()
    }

    @Synchronized
    private fun complete(isAlive: Boolean) {
        val verification = pendingVerification
        if (verification != null && !verification.isDone) {
            verification.complete(isAlive)
        }
    }
}
