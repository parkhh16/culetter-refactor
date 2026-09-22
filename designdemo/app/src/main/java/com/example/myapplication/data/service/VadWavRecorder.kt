package com.example.myapplication.data.service

import android.media.*
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max

/**
 * 마이크 → PCM16 mono → WAV 파일 저장.
 * 간단 에너지 기반 VAD:
 *  - 프레임(20ms) RMS가 threshold 이상이면 '발화 중'으로 보고 lastVoiceMs 갱신
 *  - lastVoice 이후 silenceMs(예: 900ms) 지나면 stop()
 *  - 최소 길이 minAnswerMs 충족 전엔 멈추지 않음
 */
class VadWavRecorder(
    private val sampleRate: Int = 16_000,   // Whisper에 적합
    private val threshold: Int = 1200,      // 주변 소음 민감도(높일수록 둔감)
    private val frameMs: Int = 20,
    private val silenceMs: Int = 900,
    private val minAnswerMs: Int = 1200
) {
    private var audioRecord: AudioRecord? = null
    private var job: Job? = null
    private var raf: RandomAccessFile? = null
    private val recording = AtomicBoolean(false)
    private var bytesWritten = 0L

    fun start(outFile: File, onAutoStopped: (file: File, durationMs: Long) -> Unit, onError: (String) -> Unit) {
        if (recording.get()) return

        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = max(minBuf, sampleRate) // 1s 이상 버퍼
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, // AEC/AGC에 유리
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufSize
        )

        try {
            raf = RandomAccessFile(outFile, "rw").also { writeWavHeaderPlaceholder(it, sampleRate) }
            audioRecord?.startRecording()
            recording.set(true)
        } catch (e: Exception) {
            stopInternal()
            onError("start fail: ${e.message}")
            return
        }

        job = CoroutineScope(Dispatchers.IO).launch {
            val frameBytes = (sampleRate * 2 /*bytes*/ * frameMs) / 1000
            val buf = ByteArray(frameBytes)
            var totalMs = 0L
            var lastVoiceAt = 0L
            val start = System.currentTimeMillis()

            while (recording.get()) {
                val read = audioRecord?.read(buf, 0, buf.size) ?: 0
                if (read > 0) {
                    // 파일에 쓰기
                    raf?.write(buf, 0, read)
                    bytesWritten += read

                    // RMS 계산(대충)
                    var acc = 0L
                    var count = 0
                    var i = 0
                    while (i < read) {
                        val lo = buf[i].toInt() and 0xFF
                        val hi = buf[i+1].toInt() // signed
                        val sample = (hi shl 8) or lo
                        acc += abs(sample)
                        count++
                        i += 2
                    }
                    val avg = if (count > 0) (acc / count).toInt() else 0
                    val now = System.currentTimeMillis()

                    if (avg > threshold) {
                        lastVoiceAt = now
                    }
                    totalMs = now - start

                    // 최소 길이 만족 + 무음 지속이면 자동 종료
                    if (totalMs > minAnswerMs && (now - lastVoiceAt) > silenceMs) {
                        break
                    }
                } else {
                    // 살짝 쉼
                    delay(10)
                }
            }

            val durationMs = totalMs
            stopInternal()
            onAutoStopped(outFile, durationMs)
        }
    }

    fun stop() {
        if (!recording.get()) return
        stopInternal()
    }

    private fun stopInternal() {
        recording.set(false)
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
        try {
            // WAV 헤더 마무리
            raf?.let { finalizeWavHeader(it, bytesWritten, sampleRate) }
        } catch (_: Exception) {}
        try { raf?.close() } catch (_: Exception) {}
        raf = null
        bytesWritten = 0
    }

    private fun writeWavHeaderPlaceholder(raf: RandomAccessFile, sr: Int) {
        val header = ByteArray(44) { 0 }
        // 'RIFF'
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        // chunk size (placeholder)
        // 'WAVE'
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // 'fmt '
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        // subchunk1 size 16
        writeLeInt(header, 16, 16)
        // audio format = 1 (PCM)
        writeLeShort(header, 20, 1)
        // channels = 1
        writeLeShort(header, 22, 1)
        // sample rate
        writeLeInt(header, 24, sr)
        // byte rate = sr * ch(1) * 2bytes
        writeLeInt(header, 28, sr * 2)
        // block align = ch * 2
        writeLeShort(header, 32, 2)
        // bits per sample
        writeLeShort(header, 34, 16)
        // 'data'
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        // data size placeholder
        raf.seek(0)
        raf.write(header)
        raf.seek(44)
    }

    private fun finalizeWavHeader(raf: RandomAccessFile, dataSize: Long, sr: Int) {
        val fileSize = (36 + dataSize).toInt()
        val header = ByteArray(44) { 0 }
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeLeInt(header, 4, fileSize)
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeLeInt(header, 16, 16)
        writeLeShort(header, 20, 1)
        writeLeShort(header, 22, 1)
        writeLeInt(header, 24, sr)
        writeLeInt(header, 28, sr * 2)
        writeLeShort(header, 32, 2)
        writeLeShort(header, 34, 16)
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeLeInt(header, 40, dataSize.toInt())
        raf.seek(0)
        raf.write(header)
    }

    private fun writeLeInt(buf: ByteArray, off: Int, v: Int) {
        buf[off]   = (v and 0xFF).toByte()
        buf[off+1] = ((v shr 8) and 0xFF).toByte()
        buf[off+2] = ((v shr 16) and 0xFF).toByte()
        buf[off+3] = ((v shr 24) and 0xFF).toByte()
    }
    private fun writeLeShort(buf: ByteArray, off: Int, v: Int) {
        buf[off]   = (v and 0xFF).toByte()
        buf[off+1] = ((v shr 8) and 0xFF).toByte()
    }
}
