package com.example.myapplication.data.audio

import android.media.*
import android.os.Build
import kotlin.math.max

/** PCM16 mono 스트리밍 재생 */
class RealtimeAudioPlayer(
    private val sampleRateHz: Int = 24_000
) {
    private var track: AudioTrack? = null
    private var isPlaying = false

    private fun ensureTrack() {
        if (track != null) return
        val minBuf = max(
            AudioTrack.getMinBufferSize(
                sampleRateHz,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            sampleRateHz // 1초 이상 버퍼
        )
        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRateHz)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuf)
            .build()

        // 볼륨
        try {
            if (Build.VERSION.SDK_INT >= 21) track?.setVolume(1.0f)
            else @Suppress("DEPRECATION") track?.setStereoVolume(1.0f, 1.0f)
        } catch (_: Exception) {}
    }

    fun write(pcm: ByteArray) {
        ensureTrack()
        if (!isPlaying) {
            try { track?.play() } catch (_: Exception) { return }
            isPlaying = true
        }
        track?.write(pcm, 0, pcm.size)
    }

    fun stop() {
        try { track?.stop() } catch (_: Exception) {}
        try { track?.flush() } catch (_: Exception) {}
        try { track?.release() } catch (_: Exception) {}
        track = null
        isPlaying = false
    }
}
