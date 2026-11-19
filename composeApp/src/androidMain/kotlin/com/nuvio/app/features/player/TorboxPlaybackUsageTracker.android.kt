@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.nuvio.app.features.player

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.nuvio.app.features.debrid.TorboxUsageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel

/** Counts actual network bytes read by Media3 and publishes a local usage batch every ten seconds. */
internal class TorboxPlaybackUsageTracker : TransferListener {
    private val lock = Any()
    private var pendingBytes = 0L
    private var closed = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val batches = Channel<Long>(Channel.UNLIMITED)
    private val writerJob = scope.launch {
        for (bytes in batches) {
            try {
                TorboxUsageRepository.recordStreamedBytes(bytes)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Unit
            }
        }
        scope.cancel()
    }
    private val flushJob = scope.launch {
        while (isActive) {
            delay(10_000L)
            flush()
        }
    }

    override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
    override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit
    override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) = Unit

    override fun onBytesTransferred(
        source: DataSource,
        dataSpec: DataSpec,
        isNetwork: Boolean,
        bytesTransferred: Int,
    ) {
        if (!isNetwork || bytesTransferred <= 0) return
        synchronized(lock) {
            if (closed) return
            pendingBytes = if (Long.MAX_VALUE - pendingBytes < bytesTransferred) {
                Long.MAX_VALUE
            } else {
                pendingBytes + bytesTransferred
            }
        }
    }

    fun flush() {
        val bytes = synchronized(lock) {
            pendingBytes.also { pendingBytes = 0L }
        }
        if (bytes > 0L) batches.trySend(bytes)
    }

    fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
        }
        flushJob.cancel()
        flush()
        batches.close()
    }
}
