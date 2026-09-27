package com.discipline309.app

import android.content.Context
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.DetectionMode
import com.rementia.openwakeword.lib.model.WakeWordModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Real on-device openWakeWord adapter for Maya.
 * The model is bundled into the APK by the Gradle download task.
 */
class OpenWakeWordAdapter(
    private val context: Context,
    private val onDetected: () -> Unit,
    private val onError: (Throwable) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var engine: WakeWordEngine? = null
    private var collectionJob: Job? = null

    fun start() {
        try {
            stop()
            val models = listOf(
                WakeWordModel(
                    name = "Maya",
                    modelPath = "maya.onnx",
                    threshold = 0.50f
                )
            )
            val e = WakeWordEngine(
                context = context,
                models = models,
                detectionMode = DetectionMode.SINGLE_BEST,
                detectionCooldownMs = 2000L,
                scope = scope
            )
            engine = e
            collectionJob = scope.launch {
                e.detections.collectLatest {
                    onDetected()
                }
            }
            e.start()
        } catch (t: Throwable) {
            onError(t)
        }
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
        engine?.release()
        engine = null
    }
}
