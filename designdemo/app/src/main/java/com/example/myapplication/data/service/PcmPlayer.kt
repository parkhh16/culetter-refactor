package com.example.myapplication.data.service

import android.media.*

/** WAV(PCM16, mono) 스트림을 AudioTrack로 재생 */
class PcmPlayer {
    private var track: AudioTrack? = null
    private var currentSr: Int = 0
    private var playing = false

    @Synchronized
    fun start(sampleRate: Int) {
        if (track != null && currentSr == sampleRate) {
            if (!playing) {
                track?.play()
                playing = true
            }
            return
        }
        stopAndRelease()
        currentSr = sampleRate

        val minBuf = AudioTrack.getMinBufferSize(
            currentSr,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // 언더런 방지를 위해 버퍼 여유 (최소 *2 이상 권장)
        val bufSize = (minBuf.coerceAtLeast(8 * 1024)) * 2

        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // 통신 경로 DSP(AEC/AGC/NS) 개입 회피를 위해 MEDIA 사용
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(currentSr)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufSize)
            .build()

        track?.play()
        playing = true
    }

    @Synchronized
    fun write(pcm: ByteArray, off: Int = 0, len: Int = pcm.size) {
        if (len <= 0) return
        if (!playing) {
            track?.play()
            playing = true
        }
        var writtenTotal = 0
        while (writtenTotal < len) {
            val n = track?.write(
                pcm,
                off + writtenTotal,
                len - writtenTotal,
                AudioTrack.WRITE_BLOCKING
            ) ?: break
            if (n <= 0) break
            writtenTotal += n
        }
    }

    @Synchronized
    fun stopAndRelease() {
        try { track?.stop() } catch (_: Exception) {}
        try { track?.flush() } catch (_: Exception) {}
        try { track?.release() } catch (_: Exception) {}
        track = null
        playing = false
        currentSr = 0
    }
}
