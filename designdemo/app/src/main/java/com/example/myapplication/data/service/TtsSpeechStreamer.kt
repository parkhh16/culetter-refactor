package com.example.myapplication.data.service

import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * OpenAI /v1/audio/speech
 * - response_format=wav 로 받아서 WAV 헤더 파싱 후 data chunk만 AudioTrack(PcmPlayer)에 흘림
 * - 프레임 경계(PCM16: mono=2B, stereo=4B) 정렬(carry)로 노이즈/팝음 방지
 */
class TtsSpeechStreamer(
    private val apiKey: String,
    private val player: PcmPlayer,
    private val voice: String = "alloy",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // 스트리밍
        .build()
) {
    private var job: Job? = null
    private var call: Call? = null

    // 프레임 정렬을 위한 carry 버퍼
    private var monoCarry = ByteArray(0)
    private var stereoCarry = ByteArray(0)

    fun stop() {
        try { job?.cancel() } catch (_: Exception) {}
        try { call?.cancel() } catch (_: Exception) {}
        job = null
        call = null
        monoCarry = ByteArray(0)
        stereoCarry = ByteArray(0)
        // player.stopAndRelease() 는 화면/상위에서 적절히 호출
    }

    fun speak(
        text: String,
        onStart: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        stop()
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val bodyJson = JSONObject()
                    .put("model", "gpt-4o-mini-tts")
                    .put("voice", voice)
                    .put("input", text)
                    .put("response_format", "wav")
                    .put("sample_rate", 24_000)

                val req = Request.Builder()
                    .url("https://api.openai.com/v1/audio/speech")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "audio/wav")
                    .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                call = client.newCall(req)
                val resp = call!!.execute()
                if (!resp.isSuccessful) {
                    val err = resp.body?.string()
                    onError?.invoke("TTS HTTP ${resp.code} ${err ?: ""}")
                    return@launch
                }

                resp.body?.byteStream().use { input ->
                    if (input == null) {
                        onError?.invoke("TTS empty body")
                        return@launch
                    }
                    streamWavToPlayer(input, onStart)
                    onDone?.invoke()
                }
            } catch (e: Exception) {
                onError?.invoke("TTS stream error: ${e.message}")
            }
        }
    }

    /** WAV 헤더를 파싱해서 data chunk 이후부터 PCM을 player에 씁니다. */
    private fun streamWavToPlayer(input: InputStream, onStart: (() -> Unit)?) {
        val headerBuf = ByteArrayOutputStream()
        val temp = ByteArray(4096)
        var headerParsed = false
        var dataOffset = 0
        var sampleRate = 24_000
        var channels = 1
        var bits = 16

        while (true) {
            val n = input.read(temp)
            if (n <= 0) break

            if (!headerParsed) {
                headerBuf.write(temp, 0, n)
                val bytes = headerBuf.toByteArray()
                val info = parseWavHeader(bytes)
                if (info != null) {
                    headerParsed = true
                    sampleRate = info.sampleRate
                    channels = info.channels
                    bits = info.bitsPerSample
                    dataOffset = info.dataStart

                    // 플레이어 시작
                    player.start(sampleRate)

                    // 초기 팝음 줄이기: 10~20ms 정도 0 PCM 워밍업(옵션)
                    player.write(ByteArray((sampleRate / 100) * 2)) // 10ms @ mono 16bit

                    onStart?.invoke()

                    // 헤더 뒤에 이미 따라온 data 일부를 먼저 집어넣기
                    if (bytes.size > dataOffset) {
                        writePcm(bytes, dataOffset, bytes.size - dataOffset, channels, bits)
                    }
                }
            } else {
                writePcm(temp, 0, n, channels, bits)
            }
        }
    }

    private fun writePcm(src: ByteArray, off: Int, len: Int, channels: Int, bits: Int) {
        if (bits != 16 || len <= 0) return
        when (channels) {
            1 -> writeMonoAligned(src, off, len)
            2 -> writeStereoDownmixAligned(src, off, len)
            else -> { /* unsupported multichannel */ }
        }
    }

    /** mono(2바이트) 경계 정렬 후 그대로 흘림 */
    private fun writeMonoAligned(src: ByteArray, off: Int, len: Int) {
        val buf = if (monoCarry.isEmpty()) {
            src.copyOfRange(off, off + len)
        } else {
            ByteArray(monoCarry.size + len).also {
                System.arraycopy(monoCarry, 0, it, 0, monoCarry.size)
                System.arraycopy(src, off, it, monoCarry.size, len)
            }
        }
        val writable = buf.size - (buf.size % 2) // 2바이트 정렬
        if (writable > 0) player.write(buf, 0, writable)
        monoCarry = if (writable < buf.size) buf.copyOfRange(writable, buf.size) else ByteArray(0)
    }

    /** stereo(4바이트) 경계 정렬 후 (L+R)/2 다운믹스 → mono 16bit */
    private fun writeStereoDownmixAligned(src: ByteArray, off: Int, len: Int) {
        val buf = if (stereoCarry.isEmpty()) {
            src.copyOfRange(off, off + len)
        } else {
            ByteArray(stereoCarry.size + len).also {
                System.arraycopy(stereoCarry, 0, it, 0, stereoCarry.size)
                System.arraycopy(src, off, it, stereoCarry.size, len)
            }
        }
        val processBytes = buf.size / 4 * 4 // 4바이트 정렬
        if (processBytes > 0) {
            val out = ByteArray(processBytes / 2) // mono 2바이트 * 프레임수
            var i = 0
            var j = 0
            while (i < processBytes) {
                // 리틀엔디안 16-bit
                val l = ((buf[i + 1].toInt() shl 8) or (buf[i].toInt() and 0xFF)).toShort().toInt()
                val r = ((buf[i + 3].toInt() shl 8) or (buf[i + 2].toInt() and 0xFF)).toShort().toInt()
                val m = ((l + r) / 2).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                out[j] = (m and 0xFF).toByte()
                out[j + 1] = ((m ushr 8) and 0xFF).toByte()
                i += 4
                j += 2
            }
            player.write(out, 0, out.size)
        }
        stereoCarry = if (processBytes < buf.size) buf.copyOfRange(processBytes, buf.size) else ByteArray(0)
    }

    private data class WavInfo(
        val sampleRate: Int,
        val channels: Int,
        val bitsPerSample: Int,
        val dataStart: Int
    )

    /** 최소한의 WAV 파서 (RIFF/WAVE, fmt, data) */
    private fun parseWavHeader(b: ByteArray): WavInfo? {
        if (b.size < 44) return null

        fun leInt(offset: Int): Int =
            (b[offset].toInt() and 0xFF) or
                    ((b[offset + 1].toInt() and 0xFF) shl 8) or
                    ((b[offset + 2].toInt() and 0xFF) shl 16) or
                    ((b[offset + 3].toInt() and 0xFF) shl 24)

        fun leShort(offset: Int): Int =
            (b[offset].toInt() and 0xFF) or ((b[offset + 1].toInt() and 0xFF) shl 8)

        if (String(b, 0, 4) != "RIFF") return null
        if (String(b, 8, 4) != "WAVE") return null

        var off = 12
        var sr = 24_000
        var ch = 1
        var bits = 16
        var dataStart = -1

        while (off + 8 <= b.size) {
            val id = String(b, off, 4)
            val size = leInt(off + 4)
            val next = off + 8 + size
            if (next > b.size && dataStart == -1) return null // 아직 헤더 부족

            when (id) {
                "fmt " -> {
                    if (off + 8 + 16 <= b.size) {
                        /* val audioFmt = */ leShort(off + 8) // PCM=1(확장 무시)
                        ch = leShort(off + 10)
                        sr = leInt(off + 12)
                        bits = leShort(off + 22)
                    } else return null
                }
                "data" -> {
                    dataStart = off + 8
                    return WavInfo(sr, ch, bits, dataStart)
                }
            }
            off = next
        }
        return null
    }
}
