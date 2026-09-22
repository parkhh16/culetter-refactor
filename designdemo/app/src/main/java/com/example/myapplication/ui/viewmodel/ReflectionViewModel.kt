package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.*
import com.example.myapplication.data.repository.ReflectionRepository
import com.example.myapplication.data.service.WhisperWebSocketService
import com.example.myapplication.utils.FirebaseTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 회고 화면 ViewModel
 */
class ReflectionViewModel : ViewModel() {
    
    private val reflectionRepository = ReflectionRepository()
    private val firebaseTokenManager = FirebaseTokenManager()
    private val whisperService = WhisperWebSocketService()
    
    private val _uiState = MutableStateFlow(ReflectionUiState())
    val uiState: StateFlow<ReflectionUiState> = _uiState.asStateFlow()
    
    private val _currentQuestion = MutableStateFlow<String?>(null)
    val currentQuestion: StateFlow<String?> = _currentQuestion.asStateFlow()
    
    private val _questionAnswers = MutableStateFlow<List<QuestionAnswer>>(emptyList())
    val questionAnswers: StateFlow<List<QuestionAnswer>> = _questionAnswers.asStateFlow()
    
    private val _originalRecords = MutableStateFlow<List<RecordForQuestion>>(emptyList())
    val originalRecords: StateFlow<List<RecordForQuestion>> = _originalRecords.asStateFlow()
    
    /**
     * 회고 시작 (질문 생성)
     */
    fun startReflection(records: List<Record>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Firebase 토큰을 가져올 수 없습니다. 로그인이 필요합니다."
                )
                return@launch
            }
            
            // Record를 RecordForQuestion으로 변환
            val recordsForQuestion = records.map { record ->
                RecordForQuestion(
                    time = record.time,
                    text = record.transcriptText ?: record.summaryText
                )
            }
            
            _originalRecords.value = recordsForQuestion
            
            reflectionRepository.generateQuestions(token, recordsForQuestion)
                .onSuccess { questionResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        questionResponse = questionResponse,
                        currentStep = ReflectionStep.QUESTIONS,
                        error = null
                    )
                    // 첫 번째 질문 설정
                    setNextQuestion()
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "질문 생성 중 오류가 발생했습니다."
                    )
                }
        }
    }
    
    /**
     * 다음 질문 설정
     */
    private fun setNextQuestion() {
        val questionResponse = _uiState.value.questionResponse
        if (questionResponse != null) {
            val allQuestions = questionResponse.questions.flatMap { it.questions }
            val currentIndex = _questionAnswers.value.size
            
            if (currentIndex < allQuestions.size) {
                _currentQuestion.value = allQuestions[currentIndex]
            } else {
                // 모든 질문 완료
                _uiState.value = _uiState.value.copy(currentStep = ReflectionStep.SUBMITTING)
                submitReflection()
            }
        }
    }
    
    /**
     * 질문에 답변
     */
    fun answerQuestion(answer: String) {
        val question = _currentQuestion.value
        if (question != null) {
            val questionResponse = _uiState.value.questionResponse
            if (questionResponse != null) {
                // 해당 질문의 record_time 찾기
                val recordTime = questionResponse.questions.find { group ->
                    group.questions.contains(question)
                }?.recordTime ?: ""
                
                val questionAnswer = QuestionAnswer(
                    question = question,
                    answer = answer,
                    recordTime = recordTime
                )
                
                _questionAnswers.value = _questionAnswers.value + questionAnswer
                setNextQuestion()
            }
        }
    }
    
    /**
     * 회고 답변 전송
     */
    private fun submitReflection() {
        viewModelScope.launch {
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Firebase 토큰을 가져올 수 없습니다."
                )
                return@launch
            }
            
            reflectionRepository.submitReflection(
                token,
                _originalRecords.value,
                _questionAnswers.value
            ).onSuccess { reflectionResponse ->
                _uiState.value = _uiState.value.copy(
                    currentStep = ReflectionStep.SAVING,
                    reflectionResponse = reflectionResponse
                )
                saveReflectionToApp(reflectionResponse)
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = exception.message ?: "회고 전송 중 오류가 발생했습니다."
                )
            }
        }
    }
    
    /**
     * 회고를 앱에 저장
     */
    private fun saveReflectionToApp(reflectionResponse: ReflectionResponse) {
        viewModelScope.launch {
            val token = firebaseTokenManager.getCurrentUserToken()
            if (token == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Firebase 토큰을 가져올 수 없습니다."
                )
                return@launch
            }
            
            val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일 회고", Locale.getDefault())
            val title = dateFormat.format(Date())
            
            reflectionRepository.saveRetrospect(
                token,
                title,
                reflectionResponse.dailyReflection
            ).onSuccess { retrospectData ->
                _uiState.value = _uiState.value.copy(
                    currentStep = ReflectionStep.COMPLETED,
                    retrospectData = retrospectData
                )
            }
            .onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    error = exception.message ?: "회고 저장 중 오류가 발생했습니다."
                )
            }
        }
    }
    
    /**
     * 회고 재시작
     */
    fun restartReflection() {
        _uiState.value = ReflectionUiState()
        _currentQuestion.value = null
        _questionAnswers.value = emptyList()
        _originalRecords.value = emptyList()
    }
    
    /**
     * 오류 초기화
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ReflectionUiState(
    val isLoading: Boolean = false,
    val currentStep: ReflectionStep = ReflectionStep.IDLE,
    val questionResponse: QuestionResponse? = null,
    val reflectionResponse: ReflectionResponse? = null,
    val retrospectData: RetrospectData? = null,
    val error: String? = null
)

enum class ReflectionStep {
    IDLE,
    QUESTIONS,
    SUBMITTING,
    SAVING,
    COMPLETED
}
