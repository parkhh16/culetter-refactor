package com.example.myapplication.data.service

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 오디오 재생 서비스
 */
class AudioPlayerService(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioPlayerService"
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private val _playbackState = MutableStateFlow(PlaybackState.STOPPED)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    /**
     * 오디오 재생
     * @param recordId 기록 ID
     * @return 재생 성공 여부
     */
    fun playAudio(recordId: Int): Boolean {
        return try {
            val audioRecorderService = AudioRecorderService(context)
            val audioFile = audioRecorderService.getRecordingFile(recordId)
            
            if (audioFile == null || !audioFile.exists()) {
                Log.e(TAG, "Audio file not found for record ID: $recordId")
                return false
            }
            
            playAudioFile(audioFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio for record ID: $recordId", e)
            false
        }
    }
    
    /**
     * 오디오 파일 재생
     * @param audioFile 오디오 파일
     * @return 재생 성공 여부
     */
    fun playAudioFile(audioFile: File): Boolean {
        return try {
            stopAudio() // 기존 재생 중지
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener { mp ->
                    _playbackState.value = PlaybackState.PLAYING
                    mp.start()
                    Log.d(TAG, "Audio playback started: ${audioFile.absolutePath}")
                }
                setOnCompletionListener {
                    _playbackState.value = PlaybackState.STOPPED
                    Log.d(TAG, "Audio playback completed")
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _playbackState.value = PlaybackState.ERROR
                    true
                }
                prepareAsync()
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio file", e)
            _playbackState.value = PlaybackState.ERROR
            false
        }
    }
    
    /**
     * 오디오 재생 중지
     */
    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            _playbackState.value = PlaybackState.STOPPED
            Log.d(TAG, "Audio playback stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio", e)
        }
    }
    
    /**
     * 오디오 일시정지
     */
    fun pauseAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    pause()
                    _playbackState.value = PlaybackState.PAUSED
                    Log.d(TAG, "Audio playback paused")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause audio", e)
        }
    }
    
    /**
     * 오디오 재개
     */
    fun resumeAudio() {
        try {
            mediaPlayer?.apply {
                if (!isPlaying) {
                    start()
                    _playbackState.value = PlaybackState.PLAYING
                    Log.d(TAG, "Audio playback resumed")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume audio", e)
        }
    }
    
    /**
     * 현재 재생 상태 확인
     */
    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }
    
    /**
     * 리소스 정리
     */
    fun cleanup() {
        stopAudio()
    }
}

enum class PlaybackState {
    STOPPED,
    PLAYING,
    PAUSED,
    ERROR
}
