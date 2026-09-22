package com.example.myapplication.data.model

import java.io.File

data class QA(
    val question: String,
    var audioFile: File? = null,   // 녹음된 WAV
    var transcript: String = ""    // Whisper 전사
)
