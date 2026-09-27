package com.barath.modelviewer.util

import android.os.SystemClock
import android.view.Choreographer

class PerformanceTracker(
    private val onStatsUpdated: (fps: Int, ramMb: Long) -> Unit
) : Choreographer.FrameCallback {

    private var isTracking = false
    private var frameCount = 0
    private var lastIntervalTimeMs = 0L

    fun start() {
        if (isTracking) return
        isTracking = true
        frameCount = 0
        lastIntervalTimeMs = SystemClock.uptimeMillis()
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        isTracking = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isTracking) return

        frameCount++
        val nowMs = SystemClock.uptimeMillis()
        val elapsedMs = nowMs - lastIntervalTimeMs

        if (elapsedMs >= 500L) {
            val fps = ((frameCount * 1000L) / elapsedMs).toInt().coerceIn(0, 120)
            val runtime = Runtime.getRuntime()
            val usedMemBytes = runtime.totalMemory() - runtime.freeMemory()
            val usedMemMb = usedMemBytes / (1024 * 1024)

            onStatsUpdated(fps, usedMemMb)

            frameCount = 0
            lastIntervalTimeMs = nowMs
        }

        Choreographer.getInstance().postFrameCallback(this)
    }
}
