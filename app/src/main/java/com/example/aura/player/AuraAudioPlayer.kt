package com.example.aura.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.aura.backend.AuraSharedBackend
import com.example.aura.backend.WavSynthHelper
import com.example.aura.model.Song
import com.example.aura.model.SongStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

class AuraAudioPlayer private constructor(private val context: Context) {

    private val backend = AuraSharedBackend.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.Main)

    private var mediaPlayer: MediaPlayer? = null
    private var progressTickerJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private var currentQueueIndex = 0

    companion object {
        private const val TAG = "AuraAudioPlayer"

        @Volatile
        private var instance: AuraAudioPlayer? = null

        fun getInstance(context: Context): AuraAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: AuraAudioPlayer(context.applicationContext).also { instance = it }
            }
        }
    }

    fun playSong(song: Song, queueList: List<Song> = listOf(song)) {
        // Enforce published status check unless admin preview
        if (song.status != SongStatus.PUBLISHED && backend.currentAdmin.value == null) {
            Log.w(TAG, "Cannot play unpublished song in normal user mode")
            return
        }

        _queue.value = queueList.ifEmpty { listOf(song) }
        currentQueueIndex = _queue.value.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        startPlayback(song)
    }

    fun playAll(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _queue.value = songs
        currentQueueIndex = startIndex.coerceIn(0, songs.size - 1)
        val song = songs[currentQueueIndex]
        startPlayback(song)
    }

    private fun startPlayback(song: Song) {
        releasePlayer()
        _currentSong.value = song
        _currentPositionMs.value = 0L
        _durationMs.value = song.durationMs

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                val file = File(song.audioUrl)
                if (file.exists()) {
                    setDataSource(file.absolutePath)
                } else if (song.audioUrl.startsWith("http")) {
                    setDataSource(context, Uri.parse(song.audioUrl))
                } else {
                    // Local file from another device that hasn't been cloud-uploaded yet:
                    Log.w(TAG, "Audio file not found on device (${song.audioUrl}). Using fallback audio stream.")
                    val fallbackFile = WavSynthHelper.createAmbientWavTrack(
                        context = context,
                        fileName = "fallback_${song.id.filter { it.isLetterOrDigit() }}.wav",
                        baseFreq = 146.83,
                        bpm = 120,
                        durationSeconds = (song.durationMs / 1000).toInt().coerceIn(30, 240)
                    )
                    setDataSource(fallbackFile.absolutePath)
                }

                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _durationMs.value = mp.duration.toLong().coerceAtLeast(song.durationMs)
                    startProgressTicker()
                    backend.recordPlay(song.id, backend.currentUser.value?.id)
                }

                setOnCompletionListener {
                    handleTrackCompletion()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what, extra=$extra")
                    _isPlaying.value = false
                    stopProgressTicker()
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaPlayer for song: ${song.title}", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: run {
            _currentSong.value?.let { playSong(it, _queue.value) }
            return
        }

        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            stopProgressTicker()
        } else {
            player.start()
            _isPlaying.value = true
            startProgressTicker()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
                stopProgressTicker()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                startProgressTicker()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_isShuffle.value && q.size > 1) {
            var nextIdx = (0 until q.size).random()
            if (nextIdx == currentQueueIndex) {
                nextIdx = (nextIdx + 1) % q.size
            }
            currentQueueIndex = nextIdx
            startPlayback(q[currentQueueIndex])
            return
        }

        if (currentQueueIndex + 1 < q.size) {
            currentQueueIndex++
            startPlayback(q[currentQueueIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            currentQueueIndex = 0
            startPlayback(q[0])
        }
    }

    fun skipPrevious() {
        val player = mediaPlayer
        if (player != null && player.currentPosition > 3000) {
            seekTo(0)
            return
        }

        val q = _queue.value
        if (q.isEmpty()) return

        if (currentQueueIndex - 1 >= 0) {
            currentQueueIndex--
            startPlayback(q[currentQueueIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            currentQueueIndex = q.size - 1
            startPlayback(q[currentQueueIndex])
        } else {
            seekTo(0)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    private fun handleTrackCompletion() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressTicker()
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                if (currentQueueIndex + 1 < _queue.value.size) {
                    skipNext()
                } else {
                    _isPlaying.value = false
                    stopProgressTicker()
                }
            }
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressTickerJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition.toLong()
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = null
    }

    private fun releasePlayer() {
        stopProgressTicker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
    }
}
