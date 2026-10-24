package com.limusic.desktop

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/**
 * Singleton bridge connecting Rust (crates/player/src/android.rs) with Android's AudioService.
 */
object AudioBridge {
    private val handler = Handler(Looper.getMainLooper())
    private var service: AudioService? = null
    private var context: Context? = null
    private var pendingAction: (() -> Unit)? = null

    init {
        try {
            System.loadLibrary("app_lib")
        } catch (e: UnsatisfiedLinkError) {
            android.util.Log.w("AudioBridge", "Could not load app_lib JNI: ${e.message}")
        }
    }

    fun attachContext(ctx: Context) {
        context = ctx.applicationContext
        ensureServiceStarted()
    }

    fun attachService(svc: AudioService) {
        service = svc
        val action = pendingAction
        pendingAction = null
        action?.invoke()
    }

    fun detachService() {
        service = null
    }

    // --- Calls from Rust into Android ---

    @JvmStatic
    fun play() {
        handler.post {
            try {
                val svc = service
                if (svc != null) {
                    svc.play()
                } else {
                    pendingAction = { service?.play() }
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in play()", e)
            }
        }
    }

    @JvmStatic
    fun pause() {
        handler.post {
            try {
                service?.pause()
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in pause()", e)
            }
        }
    }

    @JvmStatic
    fun stop() {
        handler.post {
            try {
                service?.stop()
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in stop()", e)
            }
        }
    }

    @JvmStatic
    fun seek(positionSecs: Double) {
        handler.post {
            try {
                service?.seekTo((positionSecs * 1000).toLong())
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in seek()", e)
            }
        }
    }

    @JvmStatic
    fun load(url: String, startSecs: Double, headersJson: String? = null) {
        handler.post {
            try {
                ensureServiceStarted()
                val svc = service
                if (svc != null) {
                    svc.loadUrl(url, (startSecs * 1000).toLong(), headersJson)
                } else {
                    pendingAction = {
                        service?.loadUrl(url, (startSecs * 1000).toLong(), headersJson)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in load()", e)
            }
        }
    }

    @JvmStatic
    fun setSpeed(speed: Float) {
        handler.post {
            try {
                service?.setPlaybackSpeed(speed)
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in setSpeed()", e)
            }
        }
    }

    @JvmStatic
    fun setLoop(loop: Boolean) {
        handler.post {
            try {
                service?.setLoop(loop)
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in setLoop()", e)
            }
        }
    }

    @JvmStatic
    fun setMetadata(title: String, artist: String, album: String?, coverUrl: String?) {
        handler.post {
            try {
                service?.updateMetadata(title, artist, album, coverUrl)
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in setMetadata()", e)
            }
        }
    }

    @JvmStatic
    fun setVolume(volumePercent: Int) {
        handler.post {
            try {
                val vol = (volumePercent.coerceIn(0, 100) / 100f)
                service?.setVolume(vol)
            } catch (e: Exception) {
                android.util.Log.e("AudioBridge", "Error in setVolume()", e)
            }
        }
    }

    private fun ensureServiceStarted() {
        val ctx = context ?: return
        try {
            val intent = Intent(ctx, AudioService::class.java)
            ctx.startService(intent)
        } catch (e: Exception) {
            android.util.Log.e("AudioBridge", "Could not start AudioService: ${e.message}")
        }
    }

    // --- Calls from Android into Rust JNI ---

    external fun nativeOnPosition(position: Double)
    external fun nativeOnDuration(duration: Double)
    external fun nativeOnPlaying(playing: Boolean)
    external fun nativeOnPaused(paused: Boolean)
    external fun nativeOnTrackEnded()
    external fun nativeOnTrackFailed()
    external fun nativeOnNext()
    external fun nativeOnPrevious()
    external fun nativeOnToggle()

    fun onPosition(positionSecs: Double) {
        try {
            nativeOnPosition(positionSecs)
        } catch (e: Throwable) {
            // Handled during pre-initialization or mock runs
        }
    }

    fun onDuration(durationSecs: Double) {
        try {
            nativeOnDuration(durationSecs)
        } catch (e: Throwable) {
        }
    }

    fun onPlaying(playing: Boolean) {
        try {
            nativeOnPlaying(playing)
        } catch (e: Throwable) {
        }
    }

    fun onPaused(paused: Boolean) {
        try {
            nativeOnPaused(paused)
        } catch (e: Throwable) {
        }
    }

    fun onTrackEnded() {
        try {
            nativeOnTrackEnded()
        } catch (e: Throwable) {
        }
    }

    fun onTrackFailed() {
        try {
            nativeOnTrackFailed()
        } catch (e: Throwable) {
        }
    }

    fun onNext() {
        try {
            nativeOnNext()
        } catch (e: Throwable) {
        }
    }

    fun onPrevious() {
        try {
            nativeOnPrevious()
        } catch (e: Throwable) {
        }
    }

    fun onToggle() {
        try {
            nativeOnToggle()
        } catch (e: Throwable) {
        }
    }
}
