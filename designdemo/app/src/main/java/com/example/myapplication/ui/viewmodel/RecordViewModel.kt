package com.example.myapplication.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.ApiClient
import com.example.myapplication.data.api.GptApiServiceImpl
import com.example.myapplication.data.api.WhisperApiService
import com.example.myapplication.data.model.RecordData
import com.example.myapplication.data.repository.RecordRepository
import com.example.myapplication.data.service.AudioRecorderService
import com.example.myapplication.data.service.RecordingState
import com.example.myapplication.utils.FirebaseTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 기록하기 화면 ViewModel
 */
class RecordViewModel : ViewModel() {
    
    private lateinit var audioRecorderService: AudioRecorderService
    private lateinit var whisperApiService: WhisperApiService
    private lateinit var gptApiService: GptApiServiceImpl
    private lateinit var recordRepository: RecordRepository
    private lateinit var firebaseTokenManager: FirebaseTokenManager
    
    private val _uiState = MutableStateFlow(RecordUiState())
    val uiState: StateFlow<RecordUiState> = _uiState.asStateFlow()
    
    fun initialize(context: Context) {
        audioRecorderService = AudioRecorderService(context)
        whisperApiService = WhisperApiService()
        gptApiService = GptApiServiceImpl(ApiClient.gptApiService)
        recordRepository = RecordRepository()
        firebaseTokenManager = FirebaseTokenManager()
    }
    
    /**
     * 녹음 시작
     */
    fun startRecording() {
        if (!::audioRecorderService.isInitialized) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            audioRecorderService.startRecording()
                .onSuccess { file ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRecording = true,
                        currentRecordingFile = file
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "녹음 시작에 실패했습니다."
                    )
                }
        }
    }
    
    /**
     * 녹음 중지
     */
    fun stopRecording() {
        if (!::audioRecorderService.isInitialized) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            audioRecorderService.stopRecording()
                .onSuccess { file ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRecording = false,
                        currentRecordingFile = file
                    )
                    
                    // 녹음 완료 후 자동으로 처리 시작
                    processRecording(file)
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRecording = false,
                        error = exception.message ?: "녹음 중지에 실패했습니다."
                    )
                }
        }
    }
    
    /**
     * 녹음 취소
     */
    fun cancelRecording() {
        if (!::audioRecorderService.isInitialized) return
        
        audioRecorderService.cancelRecording()
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            currentRecordingFile = null,
            error = null
        )
    }
    
    /**
     * 녹음 파일 처리 (음성 인식 + 요약 + 저장)
     */
    private fun processRecording(audioFile: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                processingStep = "음성 인식 중..."
            )
            
            // 1. Whisper API로 음성 인식
            whisperApiService.transcribeAudio(audioFile)
                .onSuccess { transcriptText ->
                    _uiState.value = _uiState.value.copy(
                        transcriptText = transcriptText,
                        processingStep = "요약 생성 중..."
                    )
                    
                    // 2. GPT API로 요약 생성 (실패 시 대체 로직 사용)
                    gptApiService.summarizeText(transcriptText)
                        .onSuccess { summaryText ->
                            _uiState.value = _uiState.value.copy(
                                summaryText = summaryText,
                                processingStep = "저장 중..."
                            )
                            
                            // 3. 백엔드에 저장
                            saveRecord(transcriptText, summaryText, audioFile)
                        }
                        .onFailure { exception ->
                            // GPT API 실패 시 대체 요약 생성
                            val fallbackSummary = createFallbackSummary(transcriptText)
                            
                            _uiState.value = _uiState.value.copy(
                                summaryText = fallbackSummary,
                                processingStep = "저장 중..."
                            )
                            
                            // 3. 백엔드에 저장 (대체 요약으로)
                            saveRecord(transcriptText, fallbackSummary, audioFile)
                        }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "음성 인식 실패: ${exception.message}"
                    )
                }
        }
    }
    
    /**
     * 기록 저장
     */
    private fun saveRecord(transcriptText: String, summaryText: String, audioFile: File) {
        viewModelScope.launch {
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Firebase 토큰을 가져올 수 없습니다."
                )
                return@launch
            }
            
            recordRepository.createRecord(token, transcriptText, summaryText)
                .onSuccess { recordData ->
                    // 녹음 파일을 ID로 저장
                    audioRecorderService.saveRecordingWithId(recordData.id)
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isCompleted = true,
                                createdRecord = recordData,
                                processingStep = "완료!"
                            )
                        }
                        .onFailure { exception ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "파일 저장 실패: ${exception.message}"
                            )
                        }
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "기록 저장 실패: ${exception.message}"
                    )
                }
        }
    }
    
    /**
     * 상태 초기화
     */
    fun resetState() {
        _uiState.value = RecordUiState()
    }
    
    /**
     * GPT API 실패 시 대체 요약 생성
     */
    private fun createFallbackSummary(transcriptText: String): String {
        return when {
            transcriptText.length <= 20 -> transcriptText
            transcriptText.length <= 50 -> transcriptText.take(20) + "..."
            else -> transcriptText.take(30) + "..."
        }
    }
}

data class RecordUiState(
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val isCompleted: Boolean = false,
    val currentRecordingFile: File? = null,
    val transcriptText: String = "",
    val summaryText: String = "",
    val createdRecord: RecordData? = null,
    val processingStep: String = "",
    val error: String? = null
)
