package com.limusic.desktop

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import org.json.JSONObject

class AudioService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var currentMetadata: MediaMetadata? = null
    private val handler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            try {
                player?.let { p ->
                    if (p.isPlaying) {
                        val posSecs = p.currentPosition / 1000.0
                        AudioBridge.onPosition(posSecs)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioService", "Error in progress loop", e)
            }
            handler.postDelayed(this, 250)
        }
    }

    override fun onCreate() {
        super.onCreate()
        AudioBridge.attachService(this)

        try {
            val exo = ExoPlayer.Builder(this).build()
            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            val durationSecs = exo.duration / 1000.0
                            if (durationSecs > 0) {
                                AudioBridge.onDuration(durationSecs)
                            }
                        }
                        Player.STATE_ENDED -> {
                            AudioBridge.onTrackEnded()
                        }
                        Player.STATE_IDLE -> {
                        }
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    AudioBridge.onPlaying(isPlaying)
                    AudioBridge.onPaused(!isPlaying)
                    if (isPlaying) {
                        handler.post(progressRunnable)
                    } else {
                        handler.removeCallbacks(progressRunnable)
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("AudioService", "ExoPlayer playback error: ${error.message}", error)
                    AudioBridge.onTrackFailed()
                }
            })

            player = exo

            // ForwardingPlayer enables next/previous track commands from MediaSession / headset / notification
            val forwardingPlayer = object : ForwardingPlayer(exo) {
                override fun seekToNext() {
                    AudioBridge.onNext()
                }

                override fun seekToPrevious() {
                    AudioBridge.onPrevious()
                }

                override fun seekToNextMediaItem() {
                    AudioBridge.onNext()
                }

                override fun seekToPreviousMediaItem() {
                    AudioBridge.onPrevious()
                }

                override fun isCommandAvailable(command: Int): Boolean {
                    if (command == Player.COMMAND_SEEK_TO_NEXT ||
                        command == Player.COMMAND_SEEK_TO_PREVIOUS ||
                        command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                        command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) {
                        return true
                    }
                    return super.isCommandAvailable(command)
                }

                override fun getAvailableCommands(): Player.Commands {
                    return super.getAvailableCommands().buildUpon()
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build()
                }
            }

            mediaSession = MediaSession.Builder(this, forwardingPlayer).build()
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error initializing MediaSessionService", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    fun loadUrl(url: String, startPositionMs: Long, headersJson: String? = null) {
        player?.let { p ->
            try {
                val httpFactory = DefaultHttpDataSource.Factory()
                    .setAllowCrossProtocolRedirects(true)
                if (!headersJson.isNullOrEmpty()) {
                    try {
                        val json = JSONObject(headersJson)
                        val map = mutableMapOf<String, String>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            map[key] = json.getString(key)
                        }
                        httpFactory.setDefaultRequestProperties(map)
                    } catch (e: Exception) {
                        android.util.Log.e("AudioService", "Error parsing headers JSON", e)
                    }
                }
                val dataSourceFactory = DefaultDataSource.Factory(this, httpFactory)
                val mediaSourceFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSourceFactory)
                val mediaItemBuilder = MediaItem.Builder().setUri(url)
                currentMetadata?.let { mediaItemBuilder.setMediaMetadata(it) }
                val mediaItem = mediaItemBuilder.build()
                val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
                p.setMediaSource(mediaSource, startPositionMs)
                p.prepare()
                p.play()
            } catch (e: Exception) {
                android.util.Log.e("AudioService", "Error in loadUrl", e)
                AudioBridge.onTrackFailed()
            }
        }
    }

    fun play() {
        try {
            player?.play()
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in play()", e)
        }
    }

    fun pause() {
        try {
            player?.pause()
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in pause()", e)
        }
    }

    fun stop() {
        try {
            player?.stop()
            handler.removeCallbacks(progressRunnable)
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in stop()", e)
        }
    }

    fun seekTo(positionMs: Long) {
        try {
            player?.seekTo(positionMs)
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in seekTo()", e)
        }
    }

    fun setVolume(vol: Float) {
        try {
            player?.volume = vol
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in setVolume()", e)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        try {
            player?.playbackParameters = PlaybackParameters(speed)
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in setPlaybackSpeed()", e)
        }
    }

    fun setLoop(loop: Boolean) {
        try {
            player?.repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in setLoop()", e)
        }
    }

    fun updateMetadata(title: String, artist: String, album: String?, coverUrl: String?) {
        try {
            val metaBuilder = MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
            if (!album.isNullOrEmpty()) {
                metaBuilder.setAlbumTitle(album)
            }
            if (!coverUrl.isNullOrEmpty()) {
                metaBuilder.setArtworkUri(Uri.parse(coverUrl))
            }
            val metadata = metaBuilder.build()
            currentMetadata = metadata
            player?.let { p ->
                val idx = p.currentMediaItemIndex
                if (idx >= 0 && idx < p.mediaItemCount) {
                    p.currentMediaItem?.let { current ->
                        val updated = current.buildUpon().setMediaMetadata(metadata).build()
                        p.replaceMediaItem(idx, updated)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in updateMetadata()", e)
        }
    }

    override fun onDestroy() {
        try {
            handler.removeCallbacks(progressRunnable)
            AudioBridge.detachService()
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioService", "Error in onDestroy()", e)
        }
        super.onDestroy()
    }
}
