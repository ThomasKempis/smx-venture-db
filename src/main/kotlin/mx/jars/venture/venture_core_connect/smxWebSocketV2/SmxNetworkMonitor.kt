package mx.jars.venture.venture_core_connect.smxWebSocketV2

import mx.jars.venture.venture_core_connect.SmxConstantsCore
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SmxNetworkMonitor {
    private val checkTimeout: Duration = Duration.ofSeconds(2)
    private val checkIntervalSeconds: Long = 3
    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(checkTimeout)
        .build()

    private var scheduler: ScheduledExecutorService? = null
    private var task: ScheduledFuture<*>? = null
    private val running = AtomicBoolean(false)

    fun start(onStatusChanged: (Boolean) -> Unit) {
        if (!running.compareAndSet(false, true)) return

        onStatusChanged(true)

        val executor = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "smx-network-monitor").apply { isDaemon = true }
        }
        scheduler = executor
        task = executor.scheduleWithFixedDelay({
            onStatusChanged(checkServer())
        }, 0, checkIntervalSeconds, TimeUnit.SECONDS)
    }

    fun stopServerChecks() {
        task?.cancel(true)
        task = null
    }

    fun dispose() {
        running.set(false)
        stopServerChecks()
        scheduler?.shutdownNow()
        scheduler = null
    }

    private fun checkServer(): Boolean {
        val url = SmxConstantsCore.urlServidorWeb
        if (url.isBlank()) return true

        return runCatching {
            val request = HttpRequest.newBuilder(URI.create(url))
                .timeout(checkTimeout)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build()
            client.send(request, HttpResponse.BodyHandlers.discarding())
            true
        }.getOrElse { true }
    }
}
