package com.nuvio.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

internal expect val isIos: Boolean

internal expect fun isReducedMotionEnabled(): Boolean

internal expect val supportsPosterNavigationMotion: Boolean
