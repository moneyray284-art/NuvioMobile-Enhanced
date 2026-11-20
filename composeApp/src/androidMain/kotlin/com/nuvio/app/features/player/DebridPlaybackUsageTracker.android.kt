@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.nuvio.app.features.player

import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import com.nuvio.app.features.debrid.DebridUsageProvider
import com.nuvio.app.features.debrid.PremiumizeUsageRepository
import com.nuvio.app.features.debrid.TorboxUsageRepository
import com.nuvio.app.features.debrid.resolveDebridUsageProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Attributes Media3's final redirected request URL to its debrid provider. */
internal class DebridPlaybackUsageTracker(
    private val preferredProviderId: String?,
) : TransferListener {
    private data class Batch(val provider: DebridUsageProvider, val bytes: Long)

    private val lock = Any()
    private var pendingTorboxBytes = 0L
    private var pendingPremiumizeBytes = 0L
    private var closed = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val batches = Channel<Batch>(Channel.UNLIMITED)
    private val writerJob = scope.launch {
        for (batch in batches) {
            try {
                when (batch.provider) {
                    DebridUsageProvider.TORBOX -> TorboxUsageRepository.recordStreamedBytes(batch.bytes)
                    DebridUsageProvider.PREMIUMIZE -> PremiumizeUsageRepository.recordStreamedBytes(batch.bytes)
                }
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
        val finalUrl = source.uri?.toString()?.takeIf { it.isNotBlank() } ?: dataSpec.uri.toString()
        val provider = resolveDebridUsageProvider(finalUrl, preferredProviderId) ?: return
        synchronized(lock) {
            if (closed) return
            when (provider) {
                DebridUsageProvider.TORBOX -> pendingTorboxBytes = pendingTorboxBytes.safeAdd(bytesTransferred.toLong())
                DebridUsageProvider.PREMIUMIZE -> pendingPremiumizeBytes = pendingPremiumizeBytes.safeAdd(bytesTransferred.toLong())
            }
        }
    }

    fun flush() {
        val batchesToSend = synchronized(lock) {
            listOfNotNull(
                pendingTorboxBytes.takeIf { it > 0L }?.let { Batch(DebridUsageProvider.TORBOX, it) },
                pendingPremiumizeBytes.takeIf { it > 0L }?.let { Batch(DebridUsageProvider.PREMIUMIZE, it) },
            ).also {
                pendingTorboxBytes = 0L
                pendingPremiumizeBytes = 0L
            }
        }
        batchesToSend.forEach(batches::trySend)
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

private fun Long.safeAdd(delta: Long): Long =
    if (Long.MAX_VALUE - this < delta) Long.MAX_VALUE else this + delta
