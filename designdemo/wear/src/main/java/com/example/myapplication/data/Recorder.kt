package com.example.myapplication.data

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class Recorder(private val ctx: Context) {
    private var mr: MediaRecorder? = null
    private var out: File? = null

    fun isRecording() = mr != null

    fun start(): File {
        if (mr != null) return out!!
        out = File(ctx.filesDir, "wear_${System.currentTimeMillis()}.m4a")
        mr = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(out!!.absolutePath)
            prepare()
            start()
        }
        return out!!
    }

    fun stop(): File? {
        val f = out
        runCatching { mr?.apply { stop(); release() } }
        mr = null
        out = null
        return f
    }

    /** 🔊 UI가 레벨을 읽어갈 때 쓰는 메서드 (0..32767) */
    fun getAmplitude(): Int = try { mr?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
}
