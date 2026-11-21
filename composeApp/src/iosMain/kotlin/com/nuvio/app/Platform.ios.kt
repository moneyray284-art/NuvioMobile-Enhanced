package com.nuvio.app

import platform.UIKit.UIDevice
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

internal actual val isIos: Boolean = true

internal actual fun isReducedMotionEnabled(): Boolean = UIAccessibilityIsReduceMotionEnabled()

internal actual val supportsPosterNavigationMotion: Boolean = false
