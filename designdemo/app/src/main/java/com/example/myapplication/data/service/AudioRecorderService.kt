package com.example.myapplication.data.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException

/**
 * 오디오 녹음 서비스
 */
class AudioRecorderService(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioRecorderService"
        private const val AUDIO_DIR = "recordings"
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    
    private val _recordingState = MutableStateFlow(RecordingState.STOPPED)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    /**
     * 녹음 시작
     * @return 녹음 파일 경로
     */
    fun startRecording(): Result<File> {
        return try {
            if (_recordingState.value == RecordingState.RECORDING) {
                return Result.failure(Exception("이미 녹음 중입니다."))
            }
            
            val audioDir = File(context.filesDir, AUDIO_DIR)
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            outputFile = File(audioDir, "temp_recording_$timestamp.m4a")
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile!!.absolutePath)
                
                try {
                    prepare()
                    start()
                    _recordingState.value = RecordingState.RECORDING
                    Log.d(TAG, "Recording started: ${outputFile!!.absolutePath}")
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to start recording", e)
                    return Result.failure(e)
                }
            } ?: return Result.failure(Exception("MediaRecorder 초기화 실패"))
            
            Result.success(outputFile!!)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            Result.failure(e)
        }
    }
    
    /**
     * 녹음 중지
     * @return 녹음된 파일
     */
    fun stopRecording(): Result<File> {
        return try {
            if (_recordingState.value != RecordingState.RECORDING) {
                return Result.failure(Exception("녹음 중이 아닙니다."))
            }
            
            mediaRecorder?.apply {
                try {
                    stop()
                    release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping MediaRecorder", e)
                }
            }
            
            mediaRecorder = null
            _recordingState.value = RecordingState.STOPPED
            
            val file = outputFile
            if (file != null && file.exists()) {
                Log.d(TAG, "Recording stopped: ${file.absolutePath}")
                Result.success(file)
            } else {
                Result.failure(Exception("녹음 파일을 찾을 수 없습니다."))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            _recordingState.value = RecordingState.STOPPED
            Result.failure(e)
        }
    }
    
    /**
     * 녹음 취소 (임시 파일 삭제)
     */
    fun cancelRecording() {
        try {
            if (_recordingState.value == RecordingState.RECORDING) {
                mediaRecorder?.apply {
                    try {
                        stop()
                        release()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping MediaRecorder during cancel", e)
                    }
                }
                mediaRecorder = null
                _recordingState.value = RecordingState.STOPPED
            }
            
            outputFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "Cancelled recording, deleted temp file: ${file.absolutePath}")
                }
            }
            outputFile = null
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recording", e)
        }
    }
    
    /**
     * 녹음 파일을 최종 ID로 저장
     * @param recordId 기록 ID
     * @return 최종 저장된 파일
     */
    fun saveRecordingWithId(recordId: Int): Result<File> {
        return try {
            val tempFile = outputFile
            if (tempFile == null || !tempFile.exists()) {
                return Result.failure(Exception("임시 녹음 파일이 없습니다."))
            }
            
            val audioDir = File(context.filesDir, AUDIO_DIR)
            val finalFile = File(audioDir, "record_$recordId.m4a")
            
            // 임시 파일을 최종 파일로 이동
            if (tempFile.renameTo(finalFile)) {
                outputFile = null
                Log.d(TAG, "Recording saved with ID: ${finalFile.absolutePath}")
                Result.success(finalFile)
            } else {
                Result.failure(Exception("파일 저장에 실패했습니다."))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save recording with ID", e)
            Result.failure(e)
        }
    }
    
    /**
     * 녹음 파일 삭제
     * @param recordId 기록 ID
     */
    fun deleteRecording(recordId: Int) {
        try {
            val audioDir = File(context.filesDir, AUDIO_DIR)
            val file = File(audioDir, "record_$recordId.wav")
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted recording: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording", e)
        }
    }
    
    /**
     * 녹음 파일 가져오기
     * @param recordId 기록 ID
     * @return 녹음 파일
     */
    fun getRecordingFile(recordId: Int): File? {
        val audioDir = File(context.filesDir, AUDIO_DIR)
        val file = File(audioDir, "record_$recordId.m4a")
        return if (file.exists()) file else null
    }
    
    /**
     * 모든 녹음 파일의 ID 목록 가져오기
     * @return 녹음 파일 ID 목록
     */
    fun getAllRecordingIds(): List<Int> {
        val audioDir = File(context.filesDir, AUDIO_DIR)
        if (!audioDir.exists()) {
            return emptyList()
        }
        
        return audioDir.listFiles()
            ?.filter { file ->
                file.isFile && file.name.startsWith("record_") && file.name.endsWith(".m4a")
            }
            ?.mapNotNull { file ->
                try {
                    val fileName = file.name
                    val idString = fileName.removePrefix("record_").removeSuffix(".m4a")
                    idString.toInt()
                } catch (e: NumberFormatException) {
                    null
                }
            }
            ?: emptyList()
    }
}

enum class RecordingState {
    STOPPED,
    RECORDING
}
