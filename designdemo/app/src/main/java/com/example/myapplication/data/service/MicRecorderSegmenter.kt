package com.example.myapplication.data.service

import android.content.Context
import android.media.*
import android.os.Build
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.math.log10   // ★ 추가

/**
 * 마이크 PCM16 mono를 녹음하면서 간단 VAD(에너지 기반)로
 * "말 시작 ~ 말 끝(침묵)" 구간을 자동으로 잘라 1개의 WAV 파일로 저장.
 * - 한 질문마다 start() 호출 → 침묵으로 종료되면 onSegmentDone(file) 콜백
 */
class MicRecorderSegmenter(
    private val sampleRate: Int = 16_000,
    private val frameMs: Int = 20,           // 20ms 프레임
    private val vadStartRms: Double = 400.0, // 말 시작 임계값(기기별 튜닝)
    private val vadEndSilenceMs: Int = 800,  // 연속 침묵 몇 ms 후 끝으로 간주
    private val minSpeechMs: Int = 500       // 최소 발화 길이(너무 짧으면 버림)
) {
    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private var running = false

    fun isRunning(): Boolean = running

    fun start(
        context: Context,
        outFile: File,
        onStart: (() -> Unit)? = null,
        onSegmentDone: (File) -> Unit,
        onError: (String) -> Unit,
        onLevel: (Float) -> Unit = {}   // ★ 추가: 실시간 레벨(0f..1f)
    ) {
        if (running) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            onError("마이크 권한이 없습니다.")
            return
        }

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBuf, sampleRate).coerceAtLeast(4096)

        audioRecord = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufSize)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufSize
                )
            }
        } catch (e: Exception) {
            onError("AudioRecord 생성 실패: ${e.message}")
            return
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            onError("AudioRecord 초기화 실패(state=${audioRecord?.state})")
            stop()
            return
        }

        try { audioRecord?.startRecording() } catch (e: Exception) {
            onError("녹음 시작 실패: ${e.message}")
            stop()
            return
        }

        running = true
        onStart?.invoke()

        job = CoroutineScope(Dispatchers.IO).launch {
            val frameBytes = (sampleRate * frameMs / 1000) * 2 // 16bit mono
            val buf = ByteArray(frameBytes)
            var speechStarted = false
            var speechBytes = 0L
            var silenceRunMs = 0

            // WAV 파일(헤더 placeholder 후 데이터 append)
            WavWriter(outFile, sampleRate).use { wav ->
                while (isActive && running) {
                    val n = try { audioRecord?.read(buf, 0, buf.size) ?: 0 } catch (_: Exception) { 0 }
                    if (n <= 0) {
                        delay(5)
                        continue
                    }

                    // ★ 레벨 계산 → 0..1 매핑 후 콜백
                    val rms = computeRms(buf, n)
                    val db = 20 * log10((rms / 32768.0) + 1e-9) // dBFS (음수)
                    val norm = (((db + 60.0) / 50.0).toFloat()).coerceIn(0f, 1f) // -60..-10 dBFS → 0..1
                    onLevel(norm)

                    val isVoice = rms >= vadStartRms

                    if (!speechStarted) {
                        if (isVoice) {
                            speechStarted = true
                            silenceRunMs = 0
                            wav.write(buf, 0, n)
                            speechBytes += n
                        }
                    } else {
                        wav.write(buf, 0, n)
                        speechBytes += n

                        if (isVoice) {
                            silenceRunMs = 0
                        } else {
                            silenceRunMs += frameMs
                            if (silenceRunMs >= vadEndSilenceMs) {
                                // 발화 끝
                                break
                            }
                        }
                    }
                }

                wav.finish()
                running = false

                val speechMs = (speechBytes / 2 /*bytes per sample*/ * 1000L) / sampleRate
                if (speechMs >= minSpeechMs) {
                    onSegmentDone(outFile)
                } else {
                    outFile.delete()
                    onError("발화가 너무 짧습니다(${speechMs}ms)")
                }
            }
        }
    }

    fun stop() {
        running = false
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
        try { job?.cancel() } catch (_: Exception) {}
        job = null
    }

    private fun computeRms(pcm: ByteArray, len: Int): Double {
        val bb = ByteBuffer.wrap(pcm, 0, len).order(ByteOrder.LITTLE_ENDIAN)
        var sum = 0.0
        var cnt = 0
        while (bb.remaining() >= 2) {
            val s = bb.short.toInt()
            sum += (s * s).toDouble()
            cnt++
        }
        if (cnt == 0) return 0.0
        val mean = sum / cnt
        return sqrt(mean)
    }

    /** 간단 WAV writer (16bit mono) */
    private class WavWriter(
        private val file: File,
        private val sr: Int
    ) : AutoCloseable {
        private val raf = RandomAccessFile(file, "rw")
        private var dataLen = 0L

        init {
            // placeholder header (44 bytes)
            val header = ByteArray(44) { 0 }
            raf.write(header)
        }

        fun write(b: ByteArray, off: Int, len: Int) {
            raf.write(b, off, len)
            dataLen += len
        }

        fun finish() {
            raf.seek(0)
            val totalDataLen = (36 + dataLen).toInt()

            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(totalDataLen)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // PCM fmt chunk size
            header.putShort(1) // PCM
            header.putShort(1) // mono
            header.putInt(sr)
            val byteRate = sr * 2 // mono, 16-bit
            header.putInt(byteRate)
            header.putShort(2) // block align
            header.putShort(16) // bits per sample
            header.put("data".toByteArray())
            header.putInt(dataLen.toInt())

            raf.write(header.array())
            raf.close()
        }

        override fun close() {
            try { raf.close() } catch (_: Exception) {}
        }
    }
}
