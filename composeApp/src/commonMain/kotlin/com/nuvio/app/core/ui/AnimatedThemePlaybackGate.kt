package com.nuvio.app.core.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lets the one shared gradient animator suspend while an active video is playing. */
internal object AnimatedThemePlaybackGate {
    private val _videoPlaybackActive = MutableStateFlow(false)
    val videoPlaybackActive: StateFlow<Boolean> = _videoPlaybackActive.asStateFlow()

    fun setVideoPlaybackActive(active: Boolean) {
        _videoPlaybackActive.value = active
    }
}
